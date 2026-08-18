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
      alert('✓ ' + data.message + '\n🟢 Checkpoint status updated to GREEN on map!');
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

// Leaflet Map Rendering: Scanned Checkpoints render as BRIGHT GREEN Pins (#10b981)
function initPatrolMap() {
  const defaultLat = 25.564700;
  const defaultLng = 83.977700;

  patrolMap = L.map('patrol-map').setView([defaultLat, defaultLng], 16);

  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap &copy; CARTO',
    maxZoom: 19
  }).addTo(patrolMap);

  // Fetch scan logs & checkpoints to determine green vs amber pins
  Promise.all([
    fetch('/api/checkpoints').then(res => res.json()),
    fetch('/api/scan-logs').then(res => res.json())
  ]).then(([checkpoints, scanLogs]) => {
    const scannedCheckpointIds = new Set(scanLogs.map(l => l.checkpointId));
    const routePoints = [];

    checkpoints.forEach((chk) => {
      if (chk.latitude && chk.longitude) {
        const lat = parseFloat(chk.latitude);
        const lng = parseFloat(chk.longitude);
        routePoints.push([lat, lng]);

        const isScanned = scannedCheckpointIds.has(chk.checkpointId) || scannedCheckpointIds.has(chk.qrCodeData);
        const pinColor = isScanned ? '#10b981' : '#f59e0b'; // GREEN if scanned, AMBER if pending
        const statusLabel = isScanned ? '<span style="color:#10b981; font-weight:bold;">🟢 SCANNED & VERIFIED GREEN</span>' : '<span style="color:#f59e0b; font-weight:bold;">🟡 PENDING INSPECTION</span>';

        // Draw Glowing Circle Marker
        const marker = L.circleMarker([lat, lng], {
          radius: isScanned ? 12 : 9,
          fillColor: pinColor,
          color: isScanned ? '#00ff88' : '#ffffff',
          weight: isScanned ? 3 : 2,
          opacity: 1,
          fillOpacity: 0.95
        }).addTo(patrolMap);

        marker.bindPopup(`
          <div style="color: #000; font-family: sans-serif;">
            <h6 style="margin: 0 0 4px 0;">${chk.name}</h6>
            <div style="margin-bottom: 6px;">Status: ${statusLabel}</div>
            <code style="background: #f1f5f9; padding: 2px 6px; border-radius: 4px;">${chk.qrCodeData}</code><br/>
            <small style="color: #64748b; margin-top: 4px; display: inline-block;">GPS: ${lat.toFixed(6)}, ${lng.toFixed(6)}</small>
          </div>
        `);
      }
    });

    if (routePoints.length > 1) {
      L.polyline(routePoints, {
        color: '#10b981',
        weight: 3,
        dashArray: '6, 8',
        opacity: 0.8
      }).addTo(patrolMap);
    }
  });
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
