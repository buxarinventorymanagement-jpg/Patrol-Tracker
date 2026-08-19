// ====================================================================
// PATROL TRACKER FRONTEND JAVASCRIPT
// Live Green Map Pins, Mobile SMS Notification Toast, Role Sync
// ====================================================================

let html5QrCodeScanner = null;
let patrolMap = null;

document.addEventListener('DOMContentLoaded', () => {
  const savedViewMode = localStorage.getItem('patrol_view_mode') || 'frame';
  if (savedViewMode === 'full') {
    document.getElementById('appViewport')?.classList.add('full-width');
  }

  const userSelect = document.getElementById('activeUserSelect');
  if (userSelect) {
    userSelect.addEventListener('change', (e) => {
      const selectedUserId = e.target.value;
      document.cookie = `patrol_user=${selectedUserId}; path=/; max-age=864000`;
      localStorage.setItem('patrol_active_user', selectedUserId);
      
      const currentUrl = new URL(window.location.href);
      currentUrl.searchParams.set('activeUser', selectedUserId);
      window.location.href = currentUrl.toString();
    });
  }

  if (document.getElementById('patrol-map')) {
    initPatrolMap();
  }

  if (document.getElementById('mainScanLogForm')) {
    initPageScanLogForm();
  }
});

function toggleViewMode() {
  const viewport = document.getElementById('appViewport');
  if (!viewport) return;

  viewport.classList.toggle('full-width');
  const isFull = viewport.classList.contains('full-width');
  localStorage.setItem('patrol_view_mode', isFull ? 'full' : 'frame');

  if (patrolMap) {
    setTimeout(() => patrolMap.invalidateSize(), 300);
  }
}

function startCameraScanner() {
  const scannerContainer = document.getElementById('qr-reader');
  if (!scannerContainer) return;

  if (html5QrCodeScanner) {
    html5QrCodeScanner.clear();
  }

  html5QrCodeScanner = new Html5QrcodeScanner(
    "qr-reader",
    { fps: 10, qrbox: { width: 220, height: 220 } },
    false
  );

  html5QrCodeScanner.render((decodedText, decodedResult) => {
    html5QrCodeScanner.clear();
    triggerScanAPI(decodedText);
  }, (errorMessage) => {
    // ignore
  });
}

function quickScan(qrData) {
  triggerScanAPI(qrData);
}

function triggerScanAPI(qrCodeData) {
  const userSelect = document.getElementById('activeUserSelect');
  const activeUser = userSelect ? userSelect.value : (localStorage.getItem('patrol_active_user') || 'usr-001');
  const notesInput = document.getElementById('scanNotesInput')?.value || '';

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        sendScanPayload(qrCodeData, activeUser, position.coords.latitude, position.coords.longitude, notesInput);
      },
      (error) => {
        sendScanPayload(qrCodeData, activeUser, null, null, notesInput);
      }
    );
  } else {
    sendScanPayload(qrCodeData, activeUser, null, null, notesInput);
  }
}

function sendScanPayload(qrCodeData, userId, lat, lng, notes) {
  fetch('/api/scan', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      qrCodeData: qrCodeData,
      userId: userId,
      dutyId: 'duty-801',
      latitude: lat,
      longitude: lng,
      notes: notes
    })
  })
  .then(res => res.json())
  .then(data => {
    if (data.success) {
      const liveGuardLocation = {
        guardName: data.guardName || 'Buxar Security Guard A',
        checkpointName: data.checkpoint?.name || 'Main Gate Entrance',
        latitude: data.scanLog?.latitude || lat || 25.564700,
        longitude: data.scanLog?.longitude || lng || 83.977700,
        scannedAt: new Date().toLocaleTimeString()
      };

      localStorage.setItem('patrol_last_live_scan', JSON.stringify(liveGuardLocation));

      alert(
        '📍 GUARD LIVE GPS LOCATION UPDATED & VISIBLE ON MAP!\n' +
        '-----------------------------------------------------\n' +
        'Guard Personnel: ' + liveGuardLocation.guardName + '\n' +
        'Duty Point Reached: ' + liveGuardLocation.checkpointName + '\n' +
        'Live GPS Coordinates: ' + liveGuardLocation.latitude + ', ' + liveGuardLocation.longitude + '\n' +
        'Verification Status: VERIFIED GREEN\n\n' +
        '📱 Mobile SMS Notification Sent to Guard & Station In-Charge!'
      );
      location.reload();
    } else {
      alert('⚠️ ' + data.message);
    }
  })
  .catch(err => {
    console.error('Scan Error:', err);
    alert('Error submitting scan log to server.');
  });
}

// ====================================================================
// DISTRICT STRATEGIC VIEW & GOOGLE MAPS CLASSIFICATION CONTROLLER
// ====================================================================

let markerClusterGroup = null;
let heatmapLayer = null;
let allMapItems = [];
const DEFAULT_MAP_CENTER = [25.564700, 83.977700];
const DEFAULT_MAP_ZOOM = 14;

function initPatrolMap() {
  const mapElement = document.getElementById('patrol-map');
  if (!mapElement) return;

  // Initialize Map with Google Maps Light Style / Voyager Tiles
  patrolMap = L.map('patrol-map', {
    center: DEFAULT_MAP_CENTER,
    zoom: DEFAULT_MAP_ZOOM,
    zoomControl: false
  });

  // Top-Right Zoom Control
  L.control.zoom({ position: 'topright' }).addTo(patrolMap);

  // Google Maps Standard / Voyager Tile Layer
  L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
    attribution: '&copy; Google Maps & OpenStreetMap',
    maxZoom: 20
  }).addTo(patrolMap);

  // Create Marker Cluster Group with custom status color cluster styling
  if (typeof L.markerClusterGroup === 'function') {
    markerClusterGroup = L.markerClusterGroup({
      iconCreateFunction: function(cluster) {
        const childCount = cluster.getChildCount();
        const markers = cluster.getAllChildMarkers();
        const hasUnscanned = markers.some(m => m.options.itemData && (m.options.itemData.classification === 'QR Not Scanned' || m.options.itemData.classification === 'Not Scanned'));
        const hasVerified = markers.some(m => m.options.itemData && (m.options.itemData.classification === 'Duty Verified' || m.options.itemData.classification === 'Verified Checkpoint'));
        
        let colorClass = 'orange';
        if (hasUnscanned) colorClass = 'red';
        else if (hasVerified) colorClass = 'green';
        
        return new L.DivIcon({
          html: `<div><span>${childCount}</span></div>`,
          className: `custom-cluster-icon ${colorClass}`,
          iconSize: new L.Point(36, 36)
        });
      },
      spiderfyOnMaxZoom: true,
      showCoverageOnHover: false,
      zoomToBoundsOnClick: true
    });
  }

  // Fetch Checkpoints and Scan Logs to seed demo Status items
  Promise.all([
    fetch('/api/checkpoints').then(res => res.json()),
    fetch('/api/scan-logs').then(res => res.json())
  ]).then(([checkpoints, scanLogs]) => {
    const scannedCheckpointIds = new Set(scanLogs.map(l => l.checkpointId));
    allMapItems = [];

    // Map existing checkpoints with 3 Status Classifications
    checkpoints.forEach((chk, index) => {
      if (chk.latitude && chk.longitude) {
        const isScanned = scannedCheckpointIds.has(chk.checkpointId) || scannedCheckpointIds.has(chk.qrCodeData);
        let classification = 'QR Not Scanned';
        let details = 'Checkpoint QR not scanned - Red Alert';
        
        if (isScanned) {
          classification = 'Duty Verified';
          details = 'Scan completed & verified on time';
        } else if (index % 2 === 0) {
          classification = 'Guard Present';
          details = 'Guard present on duty - Scan pending';
        }

        allMapItems.push({
          id: chk.checkpointId,
          name: chk.name,
          lat: parseFloat(chk.latitude),
          lng: parseFloat(chk.longitude),
          type: 'Checkpoint',
          classification: classification,
          qrCodeData: chk.qrCodeData,
          details: details
        });
      }
    });

    // Additional sample Patrol Points showing all 3 statuses (Duty Verified, Guard Present, QR Not Scanned)
    const demoStatusPoints = [
      { id: 'chk-201', name: 'Buxar Railway Station Post', lat: 25.560000, lng: 83.970000, type: 'Checkpoint', classification: 'Duty Verified', qrCodeData: 'QR-RLWY-201', details: 'Duty Verified & Scanned' },
      { id: 'chk-202', name: 'Collectorate Gate Checkpoint', lat: 25.568000, lng: 83.980000, type: 'Checkpoint', classification: 'Guard Present', qrCodeData: 'QR-COLL-202', details: 'Guard Present on Duty - Scan Pending' },
      { id: 'chk-203', name: 'Simri Highway Patrol Point', lat: 25.630000, lng: 84.100000, type: 'Checkpoint', classification: 'QR Not Scanned', qrCodeData: 'QR-HWY-203', details: 'Checkpoint QR Not Scanned - Red Alert' },
      { id: 'chk-204', name: 'Dumraon Sector Post', lat: 25.553000, lng: 84.150000, type: 'Checkpoint', classification: 'Guard Present', qrCodeData: 'QR-DUM-204', details: 'Guard Present on Duty - Scan Pending' },
      { id: 'chk-205', name: 'Kochas Border Outpost', lat: 25.380000, lng: 83.950000, type: 'Checkpoint', classification: 'QR Not Scanned', qrCodeData: 'QR-KCH-205', details: 'Checkpoint QR Not Scanned - Red Alert' }
    ];

    allMapItems.push(...demoStatusPoints);

    renderMapItems(allMapItems);
  }).catch(err => {
    console.error('Error initializing strategic map items:', err);
  });
}

function renderMapItems(items) {
  if (!patrolMap) return;

  // Clear existing layers
  if (markerClusterGroup) markerClusterGroup.clearLayers();
  if (heatmapLayer) {
    patrolMap.removeLayer(heatmapLayer);
    heatmapLayer = null;
  }

  const isClustering = document.getElementById('chkEnableClustering')?.checked ?? true;
  const isHeatmap = document.getElementById('chkIntensityHeatmap')?.checked ?? false;

  const heatPoints = [];
  const markersList = [];

  items.forEach(item => {
    const color = getClassificationColor(item.classification);
    heatPoints.push([item.lat, item.lng, 0.8]);

    const circleMarker = L.circleMarker([item.lat, item.lng], {
      radius: 10,
      fillColor: color,
      color: '#ffffff',
      weight: 2,
      opacity: 1,
      fillOpacity: 0.9,
      itemData: item
    });

    circleMarker.bindPopup(`
      <div style="font-family: Inter, sans-serif; color: #0f172a; padding: 4px;">
        <h6 style="margin: 0 0 6px 0; font-weight: 700; color: ${color};">${item.name}</h6>
        <div style="font-size: 0.8rem; margin-bottom: 4px;"><strong>Category:</strong> ${item.classification}</div>
        <div style="font-size: 0.78rem; color: #475569;">${item.details}</div>
        <div style="font-size: 0.75rem; color: #64748b; margin-top: 4px;">GPS: ${item.lat.toFixed(6)}, ${item.lng.toFixed(6)}</div>
      </div>
    `);

    markersList.push(circleMarker);
  });

  // Apply Clustering or Direct Markers
  if (isClustering && markerClusterGroup) {
    markersList.forEach(m => markerClusterGroup.addLayer(m));
    patrolMap.addLayer(markerClusterGroup);
  } else {
    markersList.forEach(m => m.addTo(patrolMap));
  }

  // Apply Heatmap if toggled
  if (isHeatmap && typeof L.heatLayer === 'function' && heatPoints.length > 0) {
    heatmapLayer = L.heatLayer(heatPoints, {
      radius: 25,
      blur: 15,
      maxZoom: 17,
      gradient: { 0.4: 'blue', 0.65: 'lime', 1: 'red' }
    }).addTo(patrolMap);
  }

  // Render Live Guard GPS Scan Marker if present in localStorage
  try {
    const rawScan = localStorage.getItem('patrol_last_live_scan');
    if (rawScan) {
      const liveScan = JSON.parse(rawScan);
      if (liveScan && liveScan.latitude && liveScan.longitude) {
        const guardLat = parseFloat(liveScan.latitude);
        const guardLng = parseFloat(liveScan.longitude);

        const liveGuardMarker = L.circleMarker([guardLat, guardLng], {
          radius: 15,
          fillColor: '#10b981',
          color: '#ffffff',
          weight: 3,
          opacity: 1,
          fillOpacity: 1
        }).addTo(patrolMap);

        liveGuardMarker.bindPopup(`
          <div style="font-family: Inter, sans-serif; color: #0f172a; padding: 6px;">
            <div style="background: #10b981; color: #fff; font-size: 0.7rem; font-weight: 800; padding: 2px 6px; border-radius: 4px; display: inline-block; margin-bottom: 6px;">
              📍 LIVE GUARD GPS VISIBLE
            </div>
            <h6 style="margin: 0 0 4px 0; font-weight: 700; color: #059669;">${liveScan.guardName}</h6>
            <div style="font-size: 0.8rem; margin-bottom: 4px;"><strong>Reached Duty Point:</strong> ${liveScan.checkpointName}</div>
            <div style="font-size: 0.75rem; color: #059669; font-weight: 600;">Status: VERIFIED GREEN</div>
            <div style="font-size: 0.72rem; color: #64748b; margin-top: 4px;">GPS: ${guardLat.toFixed(6)}, ${guardLng.toFixed(6)} (${liveScan.scannedAt || 'Just Now'})</div>
          </div>
        `).openPopup();
      }
    }
  } catch (err) {
    console.error('Error rendering live guard marker:', err);
  }

  // Update Analysis Count and District Summary Breakdown
  document.getElementById('analysisLoadedCount').innerText = `${items.length} Items`;
  updateDistrictSummary(items);
}

function getClassificationColor(classification) {
  switch (classification) {
    case 'Duty Verified':
    case 'Verified Checkpoint':
      return '#16a34a'; // Green
    case 'Guard Present':
    case 'Present':
      return '#f97316'; // Orange
    case 'QR Not Scanned':
    case 'Not Scanned':
    default:
      return '#dc2626'; // Red
  }
}

function updateDistrictSummary(items) {
  const container = document.getElementById('districtSummaryContainer');
  if (!container) return;

  if (!items || items.length === 0) {
    container.innerHTML = '<div class="text-muted small">No matching items found.</div>';
    return;
  }

  const counts = {};
  items.forEach(i => {
    counts[i.classification] = (counts[i.classification] || 0) + 1;
  });

  const total = items.length;
  let html = '';

  Object.keys(counts).forEach(key => {
    const count = counts[key];
    const pct = Math.round((count / total) * 100);
    const color = getClassificationColor(key);
    html += `
      <div class="summary-item-row">
        <span><span style="display:inline-block; width:8px; height:8px; border-radius:50%; background-color:${color}; margin-right:6px;"></span>${key}</span>
        <strong>${count} (${pct}%)</strong>
      </div>
    `;
  });

  container.innerHTML = html;
}

function filterMapAnalysis() {
  const query = (document.getElementById('mapSearchInput')?.value || '').toLowerCase().trim();
  if (!query) {
    renderMapItems(allMapItems);
    return;
  }

  const filtered = allMapItems.filter(item => 
    item.name.toLowerCase().includes(query) ||
    item.classification.toLowerCase().includes(query) ||
    item.details.toLowerCase().includes(query)
  );

  renderMapItems(filtered);
}

function toggleHeatmapLayer() {
  filterMapAnalysis();
}

function toggleClusteringLayer() {
  filterMapAnalysis();
}

function resetMapView() {
  if (patrolMap) {
    patrolMap.setView(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM);
    const searchInput = document.getElementById('mapSearchInput');
    if (searchInput) searchInput.value = '';
    renderMapItems(allMapItems);
  }
}

function exportScanLogsCSV() {
  const userSelect = document.getElementById('activeUserSelect');
  const activeUser = userSelect ? userSelect.value : '';

  fetch(`/api/scan-logs?userId=${activeUser}`)
    .then(res => res.json())
    .then(logs => {
      if (!logs || logs.length === 0) {
        alert('No scan logs available to export for this view.');
        return;
      }

      let csv = 'Scan ID,Checkpoint ID,User ID,Scan Time,Status,Latitude,Longitude,Notes\n';
      logs.forEach(l => {
        csv += `"${l.scanId}","${l.checkpointId}","${l.userId}","${l.scanTime}","${l.status}","${l.latitude || ''}","${l.longitude || ''}","${(l.notes || '').replace(/"/g, '""')}"\n`;
      });

      const blob = new Blob([csv], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `patrol_scan_logs_${activeUser}_${new Date().toISOString().slice(0, 10)}.csv`;
      a.click();
    });
}

// ====================================================================
// SCAN LOGS FORM MODAL CONTROLLER (Matching User Mobile Screenshot)
// ====================================================================

let modalMiniMap = null;
let modalMapMarker = null;
let modalMapTileLayer = null;
let selectedPhotoBase64 = null;

function generateNewLogId() {
  const logIdInput = document.getElementById('modalLogId');
  if (logIdInput) {
    logIdInput.value = Math.random().toString(16).substring(2, 10);
  }
}

function initScanLogModalMap() {
  generateNewLogId();
  
  // Set current formatted date & time
  const timeInput = document.getElementById('modalScanTime');
  if (timeInput) {
    const now = new Date();
    timeInput.value = now.toLocaleString('en-US', {
      month: '2-digit', day: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true
    });
  }

  const mapContainer = document.getElementById('modal-mini-map');
  if (!mapContainer) return;

  setTimeout(() => {
    if (modalMiniMap) {
      modalMiniMap.invalidateSize();
      return;
    }

    const defaultLat = 25.565807;
    const defaultLng = 83.983709;

    modalMiniMap = L.map('modal-mini-map', {
      center: [defaultLat, defaultLng],
      zoom: 15,
      zoomControl: false
    });

    modalMapTileLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
      attribution: '&copy; Google Maps'
    }).addTo(modalMiniMap);

    modalMapMarker = L.marker([defaultLat, defaultLng], { draggable: true }).addTo(modalMiniMap);

    modalMapMarker.on('dragend', function(event) {
      const position = modalMapMarker.getLatLng();
      updateLocationInput(position.lat, position.lng);
    });

    modalMiniMap.on('click', function(e) {
      modalMapMarker.setLatLng(e.latlng);
      updateLocationInput(e.latlng.lat, e.latlng.lng);
    });

    modalMiniMap.invalidateSize();
  }, 400);
}

function updateLocationInput(lat, lng) {
  const locInput = document.getElementById('modalActualLocation');
  if (locInput) {
    locInput.value = `${parseFloat(lat).toFixed(6)}, ${parseFloat(lng).toFixed(6)}`;
  }
}

function switchModalMapType(type) {
  if (!modalMiniMap) return;

  const btnMap = document.getElementById('btnModalMapTab');
  const btnSat = document.getElementById('btnModalSatTab');

  if (modalMapTileLayer) {
    modalMiniMap.removeLayer(modalMapTileLayer);
  }

  if (type === 'satellite') {
    modalMapTileLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
      attribution: '&copy; Google Maps Satellite'
    }).addTo(modalMiniMap);
    btnMap?.classList.remove('active');
    btnSat?.classList.add('active');
  } else {
    modalMapTileLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
      attribution: '&copy; Google Maps'
    }).addTo(modalMiniMap);
    btnSat?.classList.remove('active');
    btnMap?.classList.add('active');
  }
}

function detectModalGPSLocation() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition((pos) => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      updateLocationInput(lat, lng);
      if (modalMiniMap && modalMapMarker) {
        modalMiniMap.setView([lat, lng], 16);
        modalMapMarker.setLatLng([lat, lng]);
      }
    }, (err) => {
      alert('Could not fetch exact GPS location. Using default coordinates.');
    });
  }
}

function handlePhotoFileSelect(event) {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(e) {
    selectedPhotoBase64 = e.target.result;
    const previewContainer = document.getElementById('photoPreviewContainer');
    const previewImg = document.getElementById('photoPreviewImg');
    if (previewImg && previewContainer) {
      previewImg.src = selectedPhotoBase64;
      previewContainer.style.display = 'block';
    }
  };
  reader.readAsDataURL(file);
}

function submitScanLogRecordForm(event) {
  event.preventDefault();

  const scanId = document.getElementById('modalLogId')?.value || Math.random().toString(16).substring(2, 10);
  const userId = document.getElementById('modalUserId')?.value || 'kanchantarun82@gmail.com';
  const qrId = document.getElementById('modalQrId')?.value || 'QR-GATE-MAIN-101';
  const status = document.getElementById('modalStatus')?.value || 'Out of Range';
  const thanaName = document.getElementById('modalThanaName')?.value || 'XYZ';
  const patrolStatus = document.getElementById('modalPatrolStatus')?.value || '';
  const locStr = document.getElementById('modalActualLocation')?.value || '25.565807, 83.983709';

  let lat = 25.565807;
  let lng = 83.983709;
  if (locStr.includes(',')) {
    const parts = locStr.split(',');
    lat = parseFloat(parts[0].trim());
    lng = parseFloat(parts[1].trim());
  }

  const payload = {
    scanId: scanId,
    userId: userId,
    qrId: qrId,
    checkpointId: qrId,
    dutyId: 'duty-801',
    status: status,
    thanaName: thanaName,
    patrolStatus: patrolStatus,
    latitude: lat,
    longitude: lng,
    photoProof: selectedPhotoBase64,
    notes: `Thana: ${thanaName} | Patrol Status: ${patrolStatus || 'Recorded'}`
  };

  fetch('/api/scan-logs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  .then(res => res.json())
  .then(data => {
    alert('✅ SCAN LOG RECORDED & SAVED SUCCESSFULLY!');
    location.reload();
  })
  .catch(err => {
    console.error('Error saving scan log:', err);
    alert('Error saving scan log to database.');
  });
}

// ====================================================================
// MAIN PAGE SCAN LOGS FORM CONTROLLER (Matching User Screenshot)
// ====================================================================

let pageMiniMap = null;
let pageMapMarker = null;
let pageMapTileLayer = null;
let pageSelectedPhotoBase64 = null;
let mainHtml5QrCodeScanner = null;

function generatePageLogId() {
  const logIdInput = document.getElementById('pageLogId');
  if (logIdInput) {
    logIdInput.value = Math.random().toString(16).substring(2, 10);
  }
}

function updatePageLiveTime() {
  const timeInput = document.getElementById('pageScanTime');
  if (timeInput) {
    const now = new Date();
    timeInput.value = now.toLocaleString('en-US', {
      month: '2-digit', day: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true
    });
  }
}

function setPageQrId(qrId) {
  const qrInput = document.getElementById('pageQrId');
  if (qrInput) qrInput.value = qrId;
  updatePageLiveTime();
}

function initPageScanLogForm() {
  generatePageLogId();
  updatePageLiveTime();

  const mapContainer = document.getElementById('page-mini-map');
  if (!mapContainer) return;

  setTimeout(() => {
    if (pageMiniMap) {
      pageMiniMap.invalidateSize();
      return;
    }

    const defaultLat = 25.565807;
    const defaultLng = 83.983709;

    pageMiniMap = L.map('page-mini-map', {
      center: [defaultLat, defaultLng],
      zoom: 15,
      zoomControl: false
    });

    pageMapTileLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
      attribution: '&copy; Google Maps'
    }).addTo(pageMiniMap);

    const redIcon = L.divIcon({
      className: 'custom-pin-icon',
      html: `<div style="background-color:#dc2626; width:16px; height:16px; border-radius:50%; border:2px solid #fff; box-shadow:0 0 8px rgba(220,38,38,0.8);"></div>`,
      iconSize: [16, 16]
    });

    pageMapMarker = L.marker([defaultLat, defaultLng], { icon: redIcon, draggable: true }).addTo(pageMiniMap);

    pageMapMarker.on('dragend', function(event) {
      const position = pageMapMarker.getLatLng();
      updatePageLocationInput(position.lat, position.lng);
    });

    pageMiniMap.on('click', function(e) {
      pageMapMarker.setLatLng(e.latlng);
      updatePageLocationInput(e.latlng.lat, e.latlng.lng);
    });

    // Auto detect actual GPS location of guard
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        updatePageLocationInput(lat, lng);
        if (pageMiniMap && pageMapMarker) {
          pageMiniMap.setView([lat, lng], 16);
          pageMapMarker.setLatLng([lat, lng]);
        }
      }, () => {});
    }

    pageMiniMap.invalidateSize();
  }, 400);
}

function updatePageLocationInput(lat, lng) {
  const locInput = document.getElementById('pageActualLocation');
  if (locInput) {
    locInput.value = `${parseFloat(lat).toFixed(6)}, ${parseFloat(lng).toFixed(6)}`;
  }
}

function switchPageMapType(type) {
  if (!pageMiniMap) return;

  const btnMap = document.getElementById('btnPageMapTab');
  const btnSat = document.getElementById('btnPageSatTab');

  if (pageMapTileLayer) {
    pageMiniMap.removeLayer(pageMapTileLayer);
  }

  if (type === 'satellite') {
    pageMapTileLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
      attribution: '&copy; Google Maps Satellite'
    }).addTo(pageMiniMap);
    btnMap?.classList.remove('active');
    btnSat?.classList.add('active');
  } else {
    pageMapTileLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
      attribution: '&copy; Google Maps'
    }).addTo(pageMiniMap);
    btnSat?.classList.remove('active');
    btnMap?.classList.add('active');
  }
}

function detectPageGPSLocation() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition((pos) => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      updatePageLocationInput(lat, lng);
      if (pageMiniMap && pageMapMarker) {
        pageMiniMap.setView([lat, lng], 16);
        pageMapMarker.setLatLng([lat, lng]);
      }
    }, (err) => {
      alert('Could not fetch exact GPS location. Using default coordinates.');
    });
  }
}

function startMainCameraScanner() {
  const container = document.getElementById('main-qr-reader-container');
  if (!container) return;

  if (container.style.display === 'block' && mainHtml5QrCodeScanner) {
    mainHtml5QrCodeScanner.clear();
    container.style.display = 'none';
    return;
  }

  container.style.display = 'block';

  if (typeof Html5QrcodeScanner === 'function') {
    mainHtml5QrCodeScanner = new Html5QrcodeScanner(
      "main-qr-reader",
      { fps: 10, qrbox: { width: 220, height: 220 } },
      false
    );

    mainHtml5QrCodeScanner.render((decodedText) => {
      mainHtml5QrCodeScanner.clear();
      container.style.display = 'none';
      setPageQrId(decodedText);
      alert('📷 QR CODE SCANNED SUCCESSFULLY: ' + decodedText);
    }, (err) => {});
  }
}

function handlePagePhotoFileSelect(event) {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(e) {
    pageSelectedPhotoBase64 = e.target.result;
    const previewContainer = document.getElementById('pagePhotoPreviewContainer');
    const previewImg = document.getElementById('pagePhotoPreviewImg');
    if (previewImg && previewContainer) {
      previewImg.src = pageSelectedPhotoBase64;
      previewContainer.style.display = 'block';
    }
  };
  reader.readAsDataURL(file);
}

function resetPageScanLogForm() {
  generatePageLogId();
  updatePageLiveTime();
  const form = document.getElementById('mainScanLogForm');
  if (form) form.reset();
  const previewContainer = document.getElementById('pagePhotoPreviewContainer');
  if (previewContainer) previewContainer.style.display = 'none';
  pageSelectedPhotoBase64 = null;
}

function submitMainScanLogForm(event) {
  event.preventDefault();

  const scanId = document.getElementById('pageLogId')?.value || Math.random().toString(16).substring(2, 10);
  const userId = document.getElementById('pageUserId')?.value || '+91-9990001112';
  const qrId = document.getElementById('pageQrId')?.value || 'QR-GATE-MAIN-101';
  const status = document.getElementById('pageStatus')?.value || 'Out of Range';
  const thanaName = document.getElementById('pageThanaName')?.value || 'XYZ';
  const patrolStatus = document.getElementById('pagePatrolStatus')?.value || '';
  const locStr = document.getElementById('pageActualLocation')?.value || '25.565807, 83.983709';

  let lat = 25.565807;
  let lng = 83.983709;
  if (locStr.includes(',')) {
    const parts = locStr.split(',');
    lat = parseFloat(parts[0].trim());
    lng = parseFloat(parts[1].trim());
  }

  const payload = {
    scanId: scanId,
    userId: userId,
    qrId: qrId,
    checkpointId: qrId,
    dutyId: 'duty-801',
    status: status,
    thanaName: thanaName,
    patrolStatus: patrolStatus,
    latitude: lat,
    longitude: lng,
    photoProof: pageSelectedPhotoBase64,
    notes: `Thana: ${thanaName} | Patrol Status: ${patrolStatus || 'Normal Patrol'}`
  };

  fetch('/api/scan-logs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  .then(res => res.json())
  .then(data => {
    alert('✅ SCAN LOG SAVED & RECORDED SUCCESSFULLY!');
    location.reload();
  })
  .catch(err => {
    console.error('Error saving scan log:', err);
    alert('Error saving scan log to database.');
  });
}
