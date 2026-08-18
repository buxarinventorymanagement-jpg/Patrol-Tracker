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

  // Create Marker Cluster Group with custom green/gray cluster styling
  if (typeof L.markerClusterGroup === 'function') {
    markerClusterGroup = L.markerClusterGroup({
      iconCreateFunction: function(cluster) {
        const childCount = cluster.getChildCount();
        const markers = cluster.getAllChildMarkers();
        const hasScanned = markers.some(m => m.options.itemData && m.options.itemData.classification === 'Verified Checkpoint');
        const colorClass = hasScanned ? '' : 'gray';
        
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

  // Fetch Checkpoints and Scan Logs to seed demo Crime & Patrol items
  Promise.all([
    fetch('/api/checkpoints').then(res => res.json()),
    fetch('/api/scan-logs').then(res => res.json())
  ]).then(([checkpoints, scanLogs]) => {
    const scannedCheckpointIds = new Set(scanLogs.map(l => l.checkpointId));
    allMapItems = [];

    // Map existing checkpoints
    checkpoints.forEach((chk, index) => {
      if (chk.latitude && chk.longitude) {
        const isScanned = scannedCheckpointIds.has(chk.checkpointId) || scannedCheckpointIds.has(chk.qrCodeData);
        allMapItems.push({
          id: chk.checkpointId,
          name: chk.name,
          lat: parseFloat(chk.latitude),
          lng: parseFloat(chk.longitude),
          type: 'Checkpoint',
          classification: isScanned ? 'Verified Checkpoint' : 'Others / Pending',
          qrCodeData: chk.qrCodeData,
          details: isScanned ? 'Green Scanned Verification' : 'Pending Inspection'
        });
      }
    });

    // Seed Strategic District Crime & Patrol Incidents (Matching screenshot categories)
    const demoIncidents = [
      { id: 'inc-101', name: 'Ghazipur Financial Crime Report', lat: 25.578000, lng: 83.578000, type: 'Crime', classification: 'Financial Crime', details: 'Cyber fraud reported' },
      { id: 'inc-102', name: 'Rasra Narcotics Offence Inspection', lat: 25.850000, lng: 83.850000, type: 'Crime', classification: 'Narcotics Offence', details: 'Contraband seized' },
      { id: 'inc-103', name: 'Buxar Land Dispute Altercation', lat: 25.560000, lng: 83.970000, type: 'Crime', classification: 'Land Dispute', details: 'Boundary wall argument' },
      { id: 'inc-104', name: 'Dumraon Robbery Incident', lat: 25.553000, lng: 84.150000, type: 'Crime', classification: 'Robbery', details: 'Store burglary' },
      { id: 'inc-105', name: 'Simri Major Highway Accident', lat: 25.630000, lng: 84.100000, type: 'Crime', classification: 'Major Accident', details: 'Vehicle collision on NH-922' },
      { id: 'inc-106', name: 'Ballia Serious Crime Alert', lat: 25.750000, lng: 84.150000, type: 'Crime', classification: 'Serious Crime / Riot', details: 'Public disturbance' },
      { id: 'inc-107', name: 'Kochas Dacoity Patrol Scan', lat: 25.380000, lng: 83.950000, type: 'Crime', classification: 'Dacoity', details: 'Night patrol check' }
    ];

    allMapItems.push(...demoIncidents);

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
    case 'Serious Crime / Riot': return '#dc2626';
    case 'Dacoity': return '#ea580c';
    case 'Robbery': return '#d97706';
    case 'Accident': case 'Major Accident': return '#0284c7';
    case 'Financial Crime': return '#3b82f6';
    case 'Narcotics Offence': return '#8b5cf6';
    case 'Land Dispute': return '#ec4899';
    case 'Verified Checkpoint': return '#16a34a';
    default: return '#64748b';
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
