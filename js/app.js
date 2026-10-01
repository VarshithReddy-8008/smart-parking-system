/**
 * ParkSmart Client Application Controller (AI-Powered Extension)
 * Handles live REST API communication with Spring Boot backend, 
 * with a full offline sandbox simulation fallback.
 */

const PROD_API_BASE = 'https://smart-parking-system-production-d4f0.up.railway.app/api/parking';
const API_BASE = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && window.location.port === '8080'
    ? 'http://localhost:8080/api/parking'
    : (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
        ? (localStorage.getItem('API_BASE') || PROD_API_BASE)
        : PROD_API_BASE;
let useMockData = false;

/**
 * Generates ISO date string in local time without UTC offset shifts
 */
function getLocalISOString(d = new Date()) {
    const pad = (num) => String(num).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

/**
 * Formats date values (ISO strings, arrays, timestamps) safely into readable local date and time strings.
 * Preserves exact year, month, day, hour, minute, second without inaccurate UTC day/month shifts.
 */
function formatDateTime(dateInput) {
    if (!dateInput) return '--';
    
    let year, month, day, hours, minutes, seconds;
    let d;

    if (Array.isArray(dateInput)) {
        [year, month, day, hours = 0, minutes = 0, seconds = 0] = dateInput;
        d = new Date(year, month - 1, day, hours, minutes, seconds);
    } else if (typeof dateInput === 'string') {
        const match = dateInput.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})(?:[T ](\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?/);
        if (match) {
            year = parseInt(match[1], 10);
            month = parseInt(match[2], 10) - 1; // 0-indexed month in JS
            day = parseInt(match[3], 10);
            hours = match[4] !== undefined ? parseInt(match[4], 10) : 0;
            minutes = match[5] !== undefined ? parseInt(match[5], 10) : 0;
            seconds = match[6] !== undefined ? parseInt(match[6], 10) : 0;
            
            if (dateInput.includes('Z') || /[+-]\d{2}:?\d{2}$/.test(dateInput)) {
                d = new Date(dateInput);
            } else {
                d = new Date(year, month, day, hours, minutes, seconds);
            }
        } else {
            d = new Date(dateInput);
        }
    } else {
        d = new Date(dateInput);
    }

    if (isNaN(d.getTime())) return '--';

    return d.toLocaleString(undefined, {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
    });
}

/**
 * Loads or initializes mock database immediately from LocalStorage.
 */
function getInitialMockDb() {
    try {
        const stored = localStorage.getItem('PARKSMART_MOCK_DB');
        if (stored) {
            const parsed = JSON.parse(stored);
            if (parsed && Array.isArray(parsed.slots) && Array.isArray(parsed.sessions)) {
                return parsed;
            }
        }
    } catch (e) {
        console.warn('Failed to load mock DB from localStorage on script load:', e);
    }

    const defaultDb = {
        vehicleTypes: [
            { typeName: 'CAR', description: 'Standard Passenger Sedan' },
            { typeName: 'BIKE', description: 'Two-Wheelers' },
            { typeName: 'TRUCK', description: 'Heavy Commercial' },
            { typeName: 'EV', description: 'Electric charging spot' },
            { typeName: 'AMBULANCE', description: 'Ambulance Emergency' },
            { typeName: 'POLICE', description: 'Police Patrol Emergency' },
            { typeName: 'FIRE_TRUCK', description: 'Fire Engine Emergency' }
        ],
        rates: [
            { vehicleType: 'CAR', hourlyRate: 30.00, gracePeriodMinutes: 15 },
            { vehicleType: 'BIKE', hourlyRate: 15.00, gracePeriodMinutes: 10 },
            { vehicleType: 'TRUCK', hourlyRate: 50.00, gracePeriodMinutes: 20 },
            { vehicleType: 'EV', hourlyRate: 25.00, gracePeriodMinutes: 15 }
        ],
        slots: [
            // Zone A: CAR & EV (12 slots)
            { id: 1, slotNumber: 'A1', zoneName: 'Zone A', slotType: { typeName: 'CAR' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 12, isCovered: true, isDisabledFriendly: true, isPremium: true, isEmergencyReserved: false },
            { id: 2, slotNumber: 'A2', zoneName: 'Zone A', slotType: { typeName: 'CAR' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 18, isCovered: true, isDisabledFriendly: false, isPremium: true, isEmergencyReserved: false },
            { id: 3, slotNumber: 'A3', zoneName: 'Zone A', slotType: { typeName: 'CAR' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 25, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 4, slotNumber: 'A4', zoneName: 'Zone A', slotType: { typeName: 'CAR' }, isOccupied: true, isElectricCharging: false, distanceToEntrance: 32, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 5, slotNumber: 'A5', zoneName: 'Zone A', slotType: { typeName: 'CAR' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 40, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 6, slotNumber: 'A6', zoneName: 'Zone A', slotType: { typeName: 'CAR' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 48, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 7, slotNumber: 'A7', zoneName: 'Zone A', slotType: { typeName: 'CAR' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 55, isCovered: true, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 8, slotNumber: 'A8', zoneName: 'Zone A', slotType: { typeName: 'CAR' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 65, isCovered: true, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 9, slotNumber: 'A9', zoneName: 'Zone A', slotType: { typeName: 'EV' }, isOccupied: false, isElectricCharging: true, distanceToEntrance: 15, isCovered: true, isDisabledFriendly: false, isPremium: true, isEmergencyReserved: false },
            { id: 10, slotNumber: 'A10', zoneName: 'Zone A', slotType: { typeName: 'EV' }, isOccupied: false, isElectricCharging: true, distanceToEntrance: 20, isCovered: true, isDisabledFriendly: false, isPremium: true, isEmergencyReserved: false },
            { id: 11, slotNumber: 'A11', zoneName: 'Zone A', slotType: { typeName: 'EV' }, isOccupied: true, isElectricCharging: true, distanceToEntrance: 35, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 12, slotNumber: 'A12', zoneName: 'Zone A', slotType: { typeName: 'EV' }, isOccupied: false, isElectricCharging: true, distanceToEntrance: 50, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            
            // Zone B: BIKE (10 slots)
            { id: 13, slotNumber: 'B1', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 15, isCovered: true, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 14, slotNumber: 'B2', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: true, isElectricCharging: false, distanceToEntrance: 22, isCovered: true, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 15, slotNumber: 'B3', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 28, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 16, slotNumber: 'B4', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 35, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 17, slotNumber: 'B5', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 42, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 18, slotNumber: 'B6', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 50, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 19, slotNumber: 'B7', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 58, isCovered: true, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 20, slotNumber: 'B8', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 66, isCovered: true, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 21, slotNumber: 'B9', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 14, isCovered: true, isDisabledFriendly: true, isPremium: true, isEmergencyReserved: false },
            { id: 22, slotNumber: 'B10', zoneName: 'Zone B', slotType: { typeName: 'BIKE' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 20, isCovered: true, isDisabledFriendly: true, isPremium: true, isEmergencyReserved: false },

            // Zone C: TRUCK (8 slots)
            { id: 23, slotNumber: 'C1', zoneName: 'Zone C', slotType: { typeName: 'TRUCK' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 30, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 24, slotNumber: 'C2', zoneName: 'Zone C', slotType: { typeName: 'TRUCK' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 38, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 25, slotNumber: 'C3', zoneName: 'Zone C', slotType: { typeName: 'TRUCK' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 46, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 26, slotNumber: 'C4', zoneName: 'Zone C', slotType: { typeName: 'TRUCK' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 55, isCovered: false, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 27, slotNumber: 'C5', zoneName: 'Zone C', slotType: { typeName: 'TRUCK' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 64, isCovered: true, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 28, slotNumber: 'C6', zoneName: 'Zone C', slotType: { typeName: 'TRUCK' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 72, isCovered: true, isDisabledFriendly: false, isPremium: false, isEmergencyReserved: false },
            { id: 29, slotNumber: 'C7', zoneName: 'Zone C', slotType: { typeName: 'TRUCK' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 10, isCovered: true, isDisabledFriendly: false, isPremium: true, isEmergencyReserved: true },
            { id: 30, slotNumber: 'C8', zoneName: 'Zone C', slotType: { typeName: 'TRUCK' }, isOccupied: false, isElectricCharging: false, distanceToEntrance: 15, isCovered: true, isDisabledFriendly: false, isPremium: true, isEmergencyReserved: true }
        ],
        sessions: [
            {
                id: 101,
                vehicle: { id: 1, licensePlate: 'MH-12-JK-9900', vehicleType: { typeName: 'CAR' }, ownerName: 'Alice Smith', ownerContact: '9999888877' },
                parkingSlot: { id: 4, slotNumber: 'A4', zoneName: 'Zone A', slotType: { typeName: 'CAR' } },
                entryTime: getLocalISOString(new Date(Date.now() - 3600000 * 2.5)),
                status: 'ACTIVE'
            },
            {
                id: 102,
                vehicle: { id: 2, licensePlate: 'DL-04-EV-5500', vehicleType: { typeName: 'EV' }, ownerName: 'Bob Jones', ownerContact: '8888777766' },
                parkingSlot: { id: 11, slotNumber: 'A11', zoneName: 'Zone A', slotType: { typeName: 'EV' } },
                entryTime: getLocalISOString(new Date(Date.now() - 3600000 * 0.1)),
                status: 'ACTIVE'
            },
            {
                id: 103,
                vehicle: { id: 3, licensePlate: 'KA-03-MM-1234', vehicleType: { typeName: 'BIKE' }, ownerName: 'Charlie Green', ownerContact: '7777666655' },
                parkingSlot: { id: 14, slotNumber: 'B2', zoneName: 'Zone B', slotType: { typeName: 'BIKE' } },
                entryTime: getLocalISOString(new Date(Date.now() - 3600000 * 5.2)),
                status: 'ACTIVE'
            }
        ],
        payments: [],
        weather: {
            currentWeather: 'Sunny',
            temperature: 32.5,
            weatherAlert: 'Normal Conditions'
        },
        emergencyEvents: [],
        pricingTrendData: [30, 32, 35, 42, 45, 40, 38, 35, 30]
    };

    try {
        localStorage.setItem('PARKSMART_MOCK_DB', JSON.stringify(defaultDb));
    } catch (e) {}

    return defaultDb;
}

// Persistent Mock Local Storage State
let mockDb = getInitialMockDb();

function loadMockDb() {
    mockDb = getInitialMockDb();
}

function saveMockDb() {
    try {
        localStorage.setItem('PARKSMART_MOCK_DB', JSON.stringify(mockDb));
        window.dispatchEvent(new Event('parking_db_updated'));
    } catch (e) {
        console.warn('Failed to save mock DB to localStorage:', e);
    }
}

// Global Charts objects
let charts = {};

document.addEventListener('DOMContentLoaded', () => {
    detectEnvironment().then(() => {
        initEventListeners();
        loadDashboardData();
        initChatbotUI();
        initAnalyticsCharts();
    });
});

async function detectEnvironment() {
    const liveBadge = document.querySelector('.live-badge');
    try {
        const response = await fetch(`${API_BASE}/stats`, { method: 'GET' });
        if (response.ok) {
            useMockData = false;
            liveBadge.innerHTML = '<div class="live-dot" style="background-color: var(--neon-green)"></div><span>CONNECTED TO BACKEND</span>';
            liveBadge.style.borderColor = 'rgba(57, 255, 20, 0.3)';
            liveBadge.style.color = 'var(--neon-green)';
            console.log('🔌 Connected to Spring Boot live REST APIs');
        } else {
            throw new Error('Server returned error status');
        }
    } catch (err) {
        useMockData = true;
        liveBadge.innerHTML = '<div class="live-dot" style="background-color: var(--neon-yellow)"></div><span>SANDBOX SIMULATION</span>';
        liveBadge.style.borderColor = 'rgba(255, 215, 0, 0.3)';
        liveBadge.style.color = 'var(--neon-yellow)';
        console.warn('⚠️ Backend REST APIs unreachable. Operating in Sandbox Simulation Mode.');
    }
}

function initTheme() {
    const html = document.documentElement;
    let savedTheme = 'dark';
    try {
        savedTheme = localStorage.getItem('ps-theme') || 'dark';
    } catch (e) {
        console.warn("Storage access restricted. Defaulting to dark theme.", e);
    }
    html.setAttribute('data-theme', savedTheme);
    const icon = document.getElementById('theme-icon');
    if (icon) {
        icon.className = savedTheme === 'dark' ? 'ti ti-sun' : 'ti ti-moon';
    }
}

function initEventListeners() {
    initTheme();
    
    const checkInForm = document.getElementById('check-in-form');
    if (checkInForm) checkInForm.addEventListener('submit', handleCheckIn);
    
    const checkOutForm = document.getElementById('check-out-form');
    if (checkOutForm) checkOutForm.addEventListener('submit', handleCheckOut);
    
    const btnPayNow = document.getElementById('btn-pay-now');
    if (btnPayNow) btnPayNow.addEventListener('click', openPaymentModal);
    
    const paymentForm = document.getElementById('payment-form');
    if (paymentForm) paymentForm.addEventListener('submit', processPayment);
    
    const btnCloseModal = document.getElementById('btn-close-modal');
    if (btnCloseModal) btnCloseModal.addEventListener('click', closePaymentModal);
    
    const inputPlate = document.getElementById('input-plate');
    if (inputPlate) {
        inputPlate.addEventListener('change', handlePlateChange);
        inputPlate.addEventListener('input', handlePlateChange);
    }
    
    // Theme toggle
    const themeBtn = document.getElementById('theme-toggle-btn');
    if (themeBtn) {
        themeBtn.addEventListener('click', () => {
            const html = document.documentElement;
            const currentTheme = html.getAttribute('data-theme') || 'dark';
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            html.setAttribute('data-theme', newTheme);
            try {
                localStorage.setItem('ps-theme', newTheme);
            } catch (e) {}
            const icon = document.getElementById('theme-icon');
            if (icon) {
                icon.className = newTheme === 'dark' ? 'ti ti-sun' : 'ti ti-moon';
            }
            // Update charts colors on theme switch
            setTimeout(initAnalyticsCharts, 100);
        });
    }
}

async function loadDashboardData() {
    await detectEnvironment();
    loadStats();
    loadSlots();
    loadActiveSessions();
    loadWeather();
    loadDynamicPricing();
    loadEmergencyAlerts();
}

/**
 * TELEMETRY STATS
 */
async function loadStats() {
    try {
        let stats;
        if (useMockData) {
            const total = mockDb.slots.length;
            const occupied = mockDb.slots.filter(s => s.isOccupied).length;
            const available = total - occupied;
            const active = mockDb.sessions.filter(s => s.status === 'ACTIVE').length;
            stats = { totalSlots: total, availableSlots: available, occupiedSlots: occupied, activeSessions: active };
        } else {
            const res = await fetch(`${API_BASE}/stats`);
            stats = await res.json();
            const activeRes = await fetch(`${API_BASE}/sessions/active`);
            const activeSessions = await activeRes.json();
            stats.activeSessions = activeSessions.length;
        }

        document.getElementById('stats-total-slots').innerText = stats.totalSlots;
        document.getElementById('stats-available-slots').innerText = stats.availableSlots;
        document.getElementById('stats-occupied-slots').innerText = stats.occupiedSlots;
        document.getElementById('stats-active-sessions').innerText = stats.activeSessions;
        
        // Populate Occupancy Speedometer Widget
        const occupancyRate = Math.round((stats.occupiedSlots / stats.totalSlots) * 100);
        const rateLabel = document.getElementById('current-occupancy-label');
        if (rateLabel) rateLabel.innerText = `${occupancyRate}%`;
    } catch (err) {
        console.error('Failed to load stats', err);
    }
}

/**
 * COLOR CODED INTERACTIVE MAP
 */
async function loadSlots() {
    try {
        let slots;
        if (useMockData) {
            slots = mockDb.slots;
        } else {
            const res = await fetch(`${API_BASE}/slots`);
            slots = await res.json();
        }

        const zones = {};
        slots.forEach(slot => {
            if (!zones[slot.zoneName]) {
                zones[slot.zoneName] = [];
            }
            zones[slot.zoneName].push(slot);
        });

        const container = document.getElementById('slot-map-container');
        container.innerHTML = '';

        Object.keys(zones).sort().forEach(zoneName => {
            const zoneGroup = document.createElement('div');
            zoneGroup.className = 'zone-group';

            const header = document.createElement('div');
            header.className = 'zone-header';
            const occupiedCount = zones[zoneName].filter(s => s.isOccupied).length;
            const freeCount = zones[zoneName].length - occupiedCount;
            header.innerHTML = `<span>${zoneName}</span> <span style="font-size: 0.8rem; font-weight: normal;">${freeCount} Free / ${occupiedCount} Occupied</span>`;
            zoneGroup.appendChild(header);

            const grid = document.createElement('div');
            grid.className = 'slots-grid';

            zones[zoneName].forEach(slot => {
                const box = document.createElement('div');
                box.className = 'slot-box';
                
                // Color Code assignment
                if (slot.isOccupied) {
                    box.classList.add('occupied'); // Red
                } else if (slot.isEmergencyReserved) {
                    box.classList.add('emergency'); // Orange
                } else if (slot.isDisabledFriendly) {
                    box.classList.add('disabled'); // Purple
                } else if (slot.isElectricCharging) {
                    box.classList.add('ev'); // Yellow
                } else {
                    box.classList.add('available'); // Green
                }

                box.dataset.slotNumber = slot.slotNumber;
                box.dataset.occupied = slot.isOccupied;

                let badge = '';
                if (slot.isEmergencyReserved) badge = '<span class="slot-ev-badge">🚨</span>';
                else if (slot.isElectricCharging) badge = '<span class="slot-ev-badge">⚡</span>';
                else if (slot.isDisabledFriendly) badge = '<span class="slot-ev-badge">♿</span>';

                box.innerHTML = `
                    ${badge}
                    <div class="slot-number">${slot.slotNumber}</div>
                    <div class="slot-type-badge">${slot.slotType.typeName}</div>
                `;

                box.addEventListener('click', () => {
                    if (slot.isOccupied) {
                        let plate = '';
                        if (useMockData) {
                            const ses = mockDb.sessions.find(s => s.status === 'ACTIVE' && s.parkingSlot.slotNumber === slot.slotNumber);
                            plate = ses ? ses.vehicle.licensePlate : '';
                        } else {
                            const rows = document.getElementById('sessions-table-body').querySelectorAll('tr');
                            rows.forEach(row => {
                                const cells = row.querySelectorAll('td');
                                if (cells.length > 1 && cells[1].innerText === slot.slotNumber) {
                                    plate = cells[0].innerText;
                                }
                            });
                        }
                        if (plate) {
                            document.getElementById('checkout-plate').value = plate;
                            document.getElementById('checkout-plate').focus();
                            document.getElementById('checkout-plate').scrollIntoView({ behavior: 'smooth' });
                        }
                    } else {
                        document.getElementById('select-type').value = slot.slotType.typeName;
                        document.getElementById('input-plate').focus();
                        document.getElementById('input-plate').scrollIntoView({ behavior: 'smooth' });
                    }
                });

                grid.appendChild(box);
            });

            zoneGroup.appendChild(grid);
            container.appendChild(zoneGroup);
        });
    } catch (err) {
        console.error('Failed to load slots', err);
    }
}

/**
 * ACTIVE VEHICLES LOG
 */
async function loadActiveSessions() {
    try {
        let sessions;
        if (useMockData) {
            sessions = mockDb.sessions.filter(s => s.status === 'ACTIVE');
        } else {
            const res = await fetch(`${API_BASE}/sessions/active`);
            sessions = await res.json();
        }

        const tbody = document.getElementById('sessions-table-body');
        tbody.innerHTML = '';

        if (sessions.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align: center; color: var(--text-secondary); padding: 2rem;">
                        No vehicles currently parked.
                    </td>
                </tr>
            `;
            return;
        }

        sessions.forEach(session => {
            const row = document.createElement('tr');
            const entryDate = formatDateTime(session.entryTime);
            
            row.innerHTML = `
                <td style="font-weight: 700; color: var(--neon-cyan);">${session.vehicle.licensePlate}</td>
                <td><span style="border: 1px solid rgba(255,255,255,0.05); padding: 0.25rem 0.5rem; border-radius: 4px; font-weight: 600;">${session.parkingSlot.slotNumber}</span></td>
                <td>${session.vehicle.vehicleType.typeName}</td>
                <td style="color: var(--text-secondary);">${entryDate}</td>
                <td>${session.vehicle.ownerName || '<span style="color: var(--text-muted);">Guest</span>'}</td>
                <td><span class="status-badge status-active">Parked</span></td>
            `;
            tbody.appendChild(row);
        });
    } catch (err) {
        console.error('Failed to load active sessions', err);
    }
}

/**
 * WEATHER WIDGET
 */
async function loadWeather() {
    try {
        let ws;
        if (useMockData) {
            ws = mockDb.weather;
        } else {
            const res = await fetch(`${API_BASE}/weather`);
            ws = await res.json();
        }

        const weatherDesc = document.getElementById('weather-desc');
        const weatherTemp = document.getElementById('weather-temp');
        const weatherIcon = document.getElementById('weather-icon');
        const weatherAlert = document.getElementById('weather-alert-label');
        const weatherRec = document.getElementById('weather-rec-label');

        if (weatherDesc) weatherDesc.innerText = ws.currentWeather;
        if (weatherTemp) weatherTemp.innerText = `${ws.temperature.toFixed(1)}°C`;
        if (weatherAlert) {
            weatherAlert.innerText = ws.weatherAlert || 'Normal Conditions';
            weatherAlert.style.color = ws.weatherAlert.toLowerCase().includes('alert') || ws.weatherAlert.toLowerCase().includes('storm') ? 'var(--neon-red)' : 'var(--text-secondary)';
        }

        let advice = "Recommend nearest parking slots to reduce walking distance.";
        let iconClass = "ti-sun";

        switch (ws.currentWeather.toUpperCase()) {
            case "RAIN":
                advice = "🌧️ Recommend Covered/Indoor parking to protect vehicle from rain.";
                iconClass = "ti-cloud-rain";
                break;
            case "STORM":
                advice = "🌩️ Recommend Covered/Indoor parking to avoid storm hazards.";
                iconClass = "ti-cloud-storm";
                break;
            case "EXTREME HEAT":
                advice = "🔥 Recommend Shaded/Covered parking to reduce cabin heat build-up.";
                iconClass = "ti-temperature-plus";
                break;
            case "FOG":
                advice = "🌫️ Recommend Covered/Indoor parking for maximum visibility and safety.";
                iconClass = "ti-cloud-fog";
                break;
            case "CLOUDY":
                advice = "☁️ Recommend nearest parking slots to reduce walking distance.";
                iconClass = "ti-cloud";
                break;
            case "SUNNY":
            default:
                advice = "☀️ Recommend nearest parking slots to reduce walking distance.";
                iconClass = "ti-sun";
                break;
        }

        if (weatherIcon) weatherIcon.className = `ti ${iconClass} weather-icon-lg`;
        if (weatherRec) weatherRec.innerText = advice;
    } catch (err) {
        console.error('Weather load failure', err);
    }
}

/**
 * SMART DYNAMIC PRICING WIDGET
 */
async function loadDynamicPricing() {
    try {
        let details;
        if (useMockData) {
            const statsTotal = mockDb.slots.length;
            const statsOccupied = mockDb.slots.filter(s => s.isOccupied).length;
            const occupancy = Math.round((statsOccupied / statsTotal) * 100);
            
            const now = new Date();
            const hours = now.getHours();
            const peak = (hours >= 8 && hours <= 10) || (hours >= 12 && hours <= 14) || (hours >= 17 && hours <= 19);
            const weekend = now.getDay() === 0 || now.getDay() === 6;

            const rates = {};
            mockDb.rates.forEach(r => {
                let dynamicRate = r.hourlyRate;
                if (weekend) dynamicRate *= 1.2;
                if (peak) dynamicRate *= 1.3;
                if (occupancy > 80) dynamicRate *= 1.5;
                else if (occupancy > 50) dynamicRate *= 1.2;
                rates[r.vehicleType] = dynamicRate;
            });

            details = {
                occupancyPercentage: occupancy,
                isWeekend: weekend,
                isPeakHour: peak,
                priceTrend: occupancy > 70 || peak ? "UP" : occupancy < 30 ? "DOWN" : "STABLE",
                currentRates: rates,
                activeMultipliers: []
            };
            if (weekend) details.activeMultipliers.push("Weekend (+20%)");
            if (peak) details.activeMultipliers.push("Peak Hour (+30%)");
            if (occupancy > 80) details.activeMultipliers.push("High Occupancy (+50%)");
            else if (occupancy > 50) details.activeMultipliers.push("Medium Occupancy (+20%)");
        } else {
            const res = await fetch(`${API_BASE}/pricing/current`);
            details = await res.json();
        }

        // Update current pricing rates on UI
        const carPriceLabel = document.getElementById('current-car-price');
        const bikePriceLabel = document.getElementById('current-bike-price');
        const truckPriceLabel = document.getElementById('current-truck-price');
        const evPriceLabel = document.getElementById('current-ev-price');
        const multiplierList = document.getElementById('active-pricing-multipliers');
        const trendIcon = document.getElementById('pricing-trend-icon');
        const peakBadge = document.getElementById('peak-pricing-badge');

        if (carPriceLabel) carPriceLabel.innerText = `₹${Math.round(details.currentRates.CAR)}/hr`;
        if (bikePriceLabel) bikePriceLabel.innerText = `₹${Math.round(details.currentRates.BIKE)}/hr`;
        if (truckPriceLabel && details.currentRates.TRUCK !== undefined) truckPriceLabel.innerText = `₹${Math.round(details.currentRates.TRUCK)}/hr`;
        if (evPriceLabel) evPriceLabel.innerText = `₹${Math.round(details.currentRates.EV)}/hr`;

        if (multiplierList) {
            multiplierList.innerHTML = details.activeMultipliers.length > 0 
                ? details.activeMultipliers.map(m => `<li>• ${m}</li>`).join('')
                : '<li>• None (Base Rates Active)</li>';
        }

        if (trendIcon) {
            if (details.priceTrend === "UP") {
                trendIcon.className = "ti ti-trending-up";
                trendIcon.style.color = "var(--neon-red)";
            } else if (details.priceTrend === "DOWN") {
                trendIcon.className = "ti ti-trending-down";
                trendIcon.style.color = "var(--neon-green)";
            } else {
                trendIcon.className = "ti ti-arrows-right-left";
                trendIcon.style.color = "var(--neon-cyan)";
            }
        }

        if (peakBadge) {
            if (details.isPeakHour) {
                peakBadge.className = "peak-badge";
                peakBadge.innerText = "PEAK";
            } else {
                peakBadge.className = "stable-badge";
                peakBadge.innerText = "NORMAL";
            }
        }
    } catch (err) {
        console.error('Pricing engine failure', err);
    }
}

/**
 * EMERGENCY ALERTS & GATE SIMULATION
 */
async function loadEmergencyAlerts() {
    try {
        let events;
        if (useMockData) {
            events = mockDb.emergencyEvents.filter(e => e.status === "ACTIVE");
        } else {
            const res = await fetch(`${API_BASE}/emergency/active`);
            events = await res.json();
        }

        const banner = document.getElementById('emergency-alert-banner');
        const text = document.getElementById('emergency-alert-text');
        const barrier = document.getElementById('emergency-gate-barrier');

        if (events && events.length > 0) {
            const activeEvent = events[0];
            if (banner) banner.style.display = 'flex';
            if (text) {
                text.innerHTML = `
                    <div class="emergency-title">Emergency Active</div>
                    <div style="font-size: 0.8rem; color: #ff9999;">
                        ${activeEvent.vehicleType} [${activeEvent.licensePlate}] Allocated to **${activeEvent.allocatedSlot}** (Priority: ${activeEvent.priorityLevel})
                    </div>
                `;
            }
            if (barrier) {
                barrier.classList.add('open');
            }
        } else {
            if (banner) banner.style.display = 'none';
            if (barrier) barrier.classList.remove('open');
        }
    } catch (err) {
        console.error('Failed to load emergency details', err);
    }
}

/**
 * VEHICLE OPERATIONS: CHECK IN
 */
async function handleCheckIn(e) {
    e.preventDefault();
    const licensePlate = document.getElementById('input-plate').value.trim().toUpperCase();
    const vehicleType = document.getElementById('select-type').value;
    const ownerName = document.getElementById('input-owner').value.trim() || null;
    const ownerContact = document.getElementById('input-contact').value.trim() || null;

    if (!licensePlate) return;

    try {
        let recSlot = null;
        if (useMockData) {
            if (mockDb.sessions.some(s => s.vehicle.licensePlate === licensePlate && s.status === 'ACTIVE')) {
                alert(`Error: Vehicle ${licensePlate} is already checked in.`);
                return;
            }

            // Simple mock recommendation
            const isEmergencyType = ['AMBULANCE', 'POLICE', 'FIRE_TRUCK'].includes(vehicleType);
            let slot = null;
            if (isEmergencyType) {
                slot = mockDb.slots.find(s => s.isEmergencyReserved && !s.isOccupied);
            }
            if (!slot) {
                const targetType = isEmergencyType ? 'TRUCK' : vehicleType;
                slot = mockDb.slots.find(s => s.slotType.typeName === targetType && !s.isOccupied);
            }

            if (!slot) {
                alert(`Error: No available slots for ${vehicleType}`);
                return;
            }

            slot.isOccupied = true;
            recSlot = slot.slotNumber;

            const newSession = {
                id: Date.now(),
                vehicle: { id: Date.now() + 1, licensePlate, vehicleType: { typeName: vehicleType }, ownerName, ownerContact },
                parkingSlot: { id: slot.id, slotNumber: slot.slotNumber, zoneName: slot.zoneName, slotType: slot.slotType },
                entryTime: getLocalISOString(),
                status: 'ACTIVE'
            };
            mockDb.sessions.push(newSession);
            saveMockDb();

            if (isEmergencyType) {
                const event = {
                    id: Date.now(),
                    licensePlate,
                    vehicleType,
                    allocatedSlot: slot.slotNumber,
                    arrivalTime: new Date().toISOString(),
                    priorityLevel: vehicleType === 'AMBULANCE' ? 'EMERGENCY' : 'CRITICAL',
                    status: 'ACTIVE'
                };
                mockDb.emergencyEvents.push(event);
                
                // Reserve adjacent slot (mock logic)
                const prefix = slot.slotNumber.replace(/\d+/, '');
                const num = parseInt(slot.slotNumber.replace(/\D+/, ''));
                const adjSlot = mockDb.slots.find(s => s.slotNumber === prefix + (num + 1));
                if (adjSlot) adjSlot.isOccupied = true;
            }
        } else {
            const response = await fetch(`${API_BASE}/check-in`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ licensePlate, vehicleType, ownerName, ownerContact })
            });
            if (!response.ok) {
                const errData = await response.json();
                alert(`Error: ${errData.message || 'Check-in failed'}`);
                return;
            }
            const sessionData = await response.json();
            recSlot = sessionData.parkingSlot.slotNumber;
        }

        document.getElementById('check-in-form').reset();
        await loadDashboardData();
        
        // Show success alert
        alert(`🎉 Checked in successfully! Allocated Slot: ${recSlot}`);
    } catch (err) {
        console.error(err);
        alert('An unexpected error occurred during check-in.');
    }
}

/**
 * VEHICLE OPERATIONS: CHECK OUT
 */
async function handleCheckOut(e) {
    e.preventDefault();
    const licensePlate = document.getElementById('checkout-plate').value.trim().toUpperCase();
    if (!licensePlate) return;

    try {
        let checkoutData;
        if (useMockData) {
            const session = mockDb.sessions.find(s => s.vehicle.licensePlate === licensePlate && s.status === 'ACTIVE');
            if (!session) {
                alert(`Error: No active parking session found for vehicle: ${licensePlate}`);
                return;
            }

            const exitTime = getLocalISOString();
            const durationMs = new Date() - new Date(session.entryTime);
            const durationInMinutes = Math.max(1, Math.floor(durationMs / 60000));
            
            // Dynamic Pricing Calculation Mock
            const baseRateObj = mockDb.rates.find(r => r.vehicleType === session.vehicle.vehicleType.typeName);
            let baseRate = baseRateObj ? baseRateObj.hourlyRate : 30.00;
            
            const now = new Date();
            const weekend = now.getDay() === 0 || now.getDay() === 6;
            const hours = now.getHours();
            const peak = (hours >= 8 && hours <= 10) || (hours >= 12 && hours <= 14) || (hours >= 17 && hours <= 19);
            
            let multiplier = 1.0;
            if (weekend) multiplier += 0.2;
            if (peak) multiplier += 0.3;

            let hourlyFee = baseRate * multiplier;
            if (session.parkingSlot.isPremium) hourlyFee += 20;
            if (session.parkingSlot.isElectricCharging) hourlyFee += 15;
            if (session.parkingSlot.isCovered) hourlyFee += 10;
            
            let amount = 0;
            const gracePeriod = baseRateObj ? baseRateObj.gracePeriodMinutes : 15;
            if (durationInMinutes > gracePeriod) {
                const billableHours = Math.ceil(durationInMinutes / 60.0);
                amount = hourlyFee * billableHours;
            }

            const slot = mockDb.slots.find(s => s.id === session.parkingSlot.id);
            slot.isOccupied = false;
            session.status = 'COMPLETED';
            session.exitTime = exitTime;

            // Release adjacent slots if emergency
            const isEmergencyType = ['AMBULANCE', 'POLICE', 'FIRE_TRUCK'].includes(session.vehicle.vehicleType.typeName);
            if (isEmergencyType) {
                const ev = mockDb.emergencyEvents.find(e => e.licensePlate === licensePlate && e.status === 'ACTIVE');
                if (ev) ev.status = "RESOLVED";
                
                const prefix = slot.slotNumber.replace(/\d+/, '');
                const num = parseInt(slot.slotNumber.replace(/\D+/, ''));
                const adjSlot = mockDb.slots.find(s => s.slotNumber === prefix + (num + 1));
                if (adjSlot) adjSlot.isOccupied = false;
            }

            const payment = {
                id: Date.now(),
                sessionId: session.id,
                amount: amount,
                paymentStatus: amount === 0 ? 'PAID' : 'PENDING',
                paymentMethod: 'CASH'
            };
            mockDb.payments.push(payment);
            saveMockDb();

            checkoutData = {
                sessionId: session.id,
                licensePlate,
                slotNumber: slot.slotNumber,
                entryTime: session.entryTime,
                exitTime,
                durationInMinutes,
                amountDue: amount,
                paymentId: payment.id,
                paymentStatus: payment.paymentStatus
            };
        } else {
            const response = await fetch(`${API_BASE}/check-out?licensePlate=${encodeURIComponent(licensePlate)}`, {
                method: 'POST'
            });
            if (!response.ok) {
                const errData = await response.json();
                alert(`Error: ${errData.message || 'Check-out failed'}`);
                return;
            }
            checkoutData = await response.json();
        }

        document.getElementById('ticket-id-label').innerText = `TXN #${checkoutData.paymentId}`;
        document.getElementById('ticket-plate').innerText = checkoutData.licensePlate;
        document.getElementById('ticket-slot').innerText = checkoutData.slotNumber;
        document.getElementById('ticket-entry').innerText = formatDateTime(checkoutData.entryTime);
        document.getElementById('ticket-exit').innerText = formatDateTime(checkoutData.exitTime);
        document.getElementById('ticket-duration').innerText = `${checkoutData.durationInMinutes} Minutes`;
        document.getElementById('ticket-total-due').innerText = `₹${checkoutData.amountDue.toFixed(2)}`;

        const payBtn = document.getElementById('btn-pay-now');
        payBtn.dataset.paymentId = checkoutData.paymentId;
        
        if (checkoutData.amountDue === 0 || checkoutData.paymentStatus === 'PAID') {
            payBtn.innerText = 'Completed (Paid)';
            payBtn.disabled = true;
            payBtn.style.opacity = '0.5';
        } else {
            payBtn.innerText = 'Collect & Finalize Payment';
            payBtn.disabled = false;
            payBtn.style.opacity = '1';
        }

        document.getElementById('checkout-ticket-stub').style.display = 'block';
        document.getElementById('checkout-plate').value = '';
        await loadDashboardData();
    } catch (err) {
        console.error(err);
        alert('An unexpected error occurred during checkout.');
    }
}

function openPaymentModal() {
    const paymentId = document.getElementById('btn-pay-now').dataset.paymentId;
    document.getElementById('modal-payment-id').value = paymentId;
    document.getElementById('payment-modal').classList.add('active');
}

function closePaymentModal() {
    document.getElementById('payment-modal').classList.remove('active');
}

async function processPayment(e) {
    e.preventDefault();
    const paymentId = document.getElementById('modal-payment-id').value;
    const paymentMethod = document.getElementById('modal-payment-method').value;

    try {
        if (useMockData) {
            const payment = mockDb.payments.find(p => p.id == paymentId);
            if (payment) {
                payment.paymentStatus = 'PAID';
                payment.paymentMethod = paymentMethod;
                saveMockDb();
            }
        } else {
            const response = await fetch(`${API_BASE}/payments/${paymentId}/pay`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ paymentMethod })
            });
            if (!response.ok) {
                alert('Payment submission failed.');
                return;
            }
        }

        closePaymentModal();
        document.getElementById('checkout-ticket-stub').style.display = 'none';
        await loadDashboardData();
        alert('Transaction finalized. Vehicle has cleared checkout! Gate opening.');
    } catch (err) {
        console.error(err);
        alert('Failed to process payment.');
    }
}

/**
 * AI PARKING CHATBOT
 */
function initChatbotUI() {
    const chatbotToggle = document.getElementById('chatbot-toggle-btn');
    const chatbotWin = document.getElementById('chatbot-window');
    const chatClose = document.getElementById('chat-close-btn');
    const chatForm = document.getElementById('chatbot-form');
    const chatInput = document.getElementById('chat-input');
    const chatMessages = document.getElementById('chat-messages');

    if (!chatbotToggle || !chatbotWin) return;

    chatbotToggle.addEventListener('click', () => {
        chatbotWin.classList.toggle('active');
        if (chatbotWin.classList.contains('active')) {
            chatInput.focus();
            if (chatMessages.children.length <= 1) {
                appendChatMessage("bot", "Hello! I am your AI Parking Assistant. Ask me things like:\n\n• *Which slot is closest to entrance?*\n• *Cheapest parking?*\n• *Recommend best parking slot.*");
            }
        }
    });

    if (chatClose) {
        chatClose.addEventListener('click', () => {
            chatbotWin.classList.remove('active');
        });
    }

    // Suggested actions click
    document.querySelectorAll('.suggest-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const text = btn.innerText;
            submitUserChat(text);
        });
    });

    chatForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const text = chatInput.value.trim();
        if (!text) return;
        submitUserChat(text);
        chatInput.value = '';
    });
}

function appendChatMessage(sender, text) {
    const chatMessages = document.getElementById('chat-messages');
    if (!chatMessages) return;

    const msgDiv = document.createElement('div');
    msgDiv.className = `chat-msg ${sender}`;
    // Support markdown bullets
    const formattedText = text.replace(/\n/g, '<br>').replace(/•/g, '&bull;').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    msgDiv.innerHTML = formattedText;
    chatMessages.appendChild(msgDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function showTypingIndicator(show) {
    const chatMessages = document.getElementById('chat-messages');
    let indicator = document.getElementById('chat-typing-indicator');

    if (show) {
        if (!indicator) {
            indicator = document.createElement('div');
            indicator.id = 'chat-typing-indicator';
            indicator.className = 'typing-indicator';
            indicator.innerHTML = '<div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>';
            chatMessages.appendChild(indicator);
        }
        chatMessages.scrollTop = chatMessages.scrollHeight;
    } else {
        if (indicator) indicator.remove();
    }
}

async function submitUserChat(messageText) {
    appendChatMessage("user", messageText);
    showTypingIndicator(true);

    try {
        if (useMockData) {
            // Mock dynamic rule response directly in JS
            setTimeout(() => {
                showTypingIndicator(false);
                const reply = getMockChatbotReply(messageText);
                appendChatMessage("bot", reply);
            }, 800);
        } else {
            const response = await fetch(`${API_BASE}/chat`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: messageText, licensePlate: "" })
            });
            showTypingIndicator(false);
            if (response.ok) {
                const data = await response.json();
                appendChatMessage("bot", data.reply);
            } else {
                appendChatMessage("bot", "Error contacting AI Assistant. Operating locally.");
            }
        }
    } catch (err) {
        showTypingIndicator(false);
        appendChatMessage("bot", "I am having trouble connecting. Check your internet connection.");
    }
}

function getMockChatbotReply(query) {
    const q = query.toLowerCase();
    
    if (q.includes("nearest") || q.includes("closest") || q.includes("entrance")) {
        const freeSlots = mockDb.slots.filter(s => !s.isOccupied && !s.isEmergencyReserved);
        const closest = freeSlots.sort((a,b) => a.distanceToEntrance - b.distanceToEntrance)[0];
        if (closest) {
            return `Recommended Slot: **${closest.slotNumber}**\n\nReason:\n• Distance: ${closest.distanceToEntrance} meters\n• Shorter walk to main elevator\n• Price: ₹${closest.isPremium ? 50 : 30}/hr`;
        }
        return "All spaces are occupied.";
    }
    
    if (q.includes("cheapest") || q.includes("budget") || q.includes("lowest")) {
        const freeSlots = mockDb.slots.filter(s => !s.isOccupied && !s.isEmergencyReserved);
        const cheapest = freeSlots.sort((a,b) => (a.isPremium ? 50 : 30) - (b.isPremium ? 50 : 30))[0];
        if (cheapest) {
            const rate = cheapest.isPremium ? 50 : 30;
            return `Recommended Budget Slot: **${cheapest.slotNumber}**\n\nReason:\n• Dynamic Price: ₹${rate}/hour\n• Located in economy zone\n• Distance: ${cheapest.distanceToEntrance} meters`;
        }
        return "All spaces are occupied.";
    }

    if (q.includes("covered") || q.includes("rain") || q.includes("storm") || q.includes("shade")) {
        const covered = mockDb.slots.find(s => !s.isOccupied && s.isCovered && !s.isEmergencyReserved);
        if (covered) {
            return `Recommended Shaded Slot: **${covered.slotNumber}**\n\nReason:\n• Under cover canopy\n• Weather-protected\n• Walk: ${covered.distanceToEntrance}m`;
        }
        return "All covered slots are occupied.";
    }

    if (q.includes("ev") || q.includes("charging") || q.includes("charger")) {
        const ev = mockDb.slots.find(s => !s.isOccupied && s.isElectricCharging);
        if (ev) {
            return `Recommended EV Charger Spot: **${ev.slotNumber}**\n\nReason:\n• Level-2 Dynamic Charger\n• Priority parking\n• Walk: ${ev.distanceToEntrance}m`;
        }
        return "All EV charging spaces are currently occupied.";
    }

    if (q.includes("where did i park") || q.includes("where is my car") || q.includes("find my")) {
        const match = query.match(/[A-Z]{2}[-\s]?\d{2}[-\s]?[A-Z]{1,2}[-\s]?\d{4}/i);
        const plate = match ? match[0].toUpperCase() : null;
        if (plate) {
            const ses = mockDb.sessions.find(s => s.vehicle.licensePlate.includes(plate) && s.status === 'ACTIVE');
            if (ses) {
                return `Your vehicle [**${plate}**] is currently parked in **${ses.parkingSlot.slotNumber}** (${ses.parkingSlot.zoneName}).`;
            }
            return `No active sessions found for plate: ${plate}.`;
        }
        return "Please provide your vehicle's license plate number (e.g. 'Where is my car MH12AB1234?').";
    }

    if (q.includes("weather")) {
        return `Current Weather Status: **${mockDb.weather.currentWeather}** (${mockDb.weather.temperature}°C)\nAlerts: ${mockDb.weather.weatherAlert}\n\nAI Advice: Recommended to use covered slots (Zone A) today.`;
    }

    // Default recommend best
    const best = mockDb.slots.find(s => !s.isOccupied && !s.isEmergencyReserved);
    if (best) {
        return `Recommended Slot: **${best.slotNumber}**\n\nReason:\n• Distance to entrance: ${best.distanceToEntrance} meters\n• Shaded parking: ${best.isCovered ? "Yes" : "No"}\n• Low congestion zone`;
    }
    return "Welcome to ParkSmart! Ask me to recommend parking, calculate pricing, or check weather alerts.";
}

/**
 * ANALYTICS CHARTS (CHART.JS)
 */
async function initAnalyticsCharts() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const gridColor = isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)';
    const textColor = isDark ? '#9196b3' : '#6b7280';

    // Nuke existing charts if rebuilding on theme change
    Object.keys(charts).forEach(key => {
        if (charts[key]) charts[key].destroy();
    });

    const revCtx = document.getElementById('revenueChart');
    const occCtx = document.getElementById('occupancyChart');
    const typeCtx = document.getElementById('vehicleTypeChart');
    const priceCtx = document.getElementById('pricingTrendChart');

    let stats = null;
    let pricing = null;
    try {
        const [statsRes, pricingRes] = await Promise.all([
            fetch(API_BASE + '/stats'),
            fetch(API_BASE + '/pricing/current')
        ]);
        if (statsRes.ok) stats = await statsRes.json();
        if (pricingRes.ok) pricing = await pricingRes.json();
    } catch (e) {
        console.error("Failed to fetch data for charts", e);
    }

    const currentOcc = stats && stats.totalSlots > 0 ? Math.round((stats.occupiedSlots / stats.totalSlots) * 100) : 50;
    const currentPrice = pricing && pricing.currentRates && pricing.currentRates['CAR'] ? pricing.currentRates['CAR'] : 30;
    const currentRev = currentOcc * currentPrice * 10;

    const occData = [Math.max(0, currentOcc - 30), Math.max(0, currentOcc - 15), currentOcc + 5, currentOcc + 15, currentOcc - 10, currentOcc, currentOcc];
    const priceData = [Math.max(10, currentPrice - 10), Math.max(10, currentPrice - 5), currentPrice + 5, currentPrice, currentPrice + 10, currentPrice - 5, currentPrice];
    const revData = [currentRev * 0.8, currentRev * 0.9, currentRev * 1.1, currentRev, currentRev * 1.2, currentRev * 1.3, currentRev];

    const carSlots = stats ? (stats.occupiedByType['CAR'] || 0) + (stats.availableByType['CAR'] || 0) : 50;
    const bikeSlots = stats ? (stats.occupiedByType['BIKE'] || 0) + (stats.availableByType['BIKE'] || 0) : 30;
    const truckSlots = stats ? (stats.occupiedByType['TRUCK'] || 0) + (stats.availableByType['TRUCK'] || 0) : 10;
    const evSlots = stats ? (stats.occupiedByType['EV'] || 0) + (stats.availableByType['EV'] || 0) : 10;

    if (revCtx) {
        charts.revenue = new Chart(revCtx, {
            type: 'line',
            data: {
                labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
                datasets: [{
                    label: 'Revenue (₹)',
                    data: revData,
                    borderColor: '#818cf8',
                    backgroundColor: 'rgba(129, 140, 248, 0.1)',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { grid: { color: gridColor }, ticks: { color: textColor } },
                    y: { grid: { color: gridColor }, ticks: { color: textColor } }
                },
                plugins: { legend: { display: false } }
            }
        });
    }

    if (occCtx) {
        charts.occupancy = new Chart(occCtx, {
            type: 'bar',
            data: {
                labels: ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00'],
                datasets: [{
                    label: 'Occupancy %',
                    data: occData,
                    backgroundColor: 'rgba(74, 222, 128, 0.75)',
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { grid: { color: gridColor }, ticks: { color: textColor } },
                    y: { max: 100, grid: { color: gridColor }, ticks: { color: textColor } }
                },
                plugins: { legend: { display: false } }
            }
        });
    }

    if (typeCtx) {
        charts.types = new Chart(typeCtx, {
            type: 'doughnut',
            data: {
                labels: ['Car', 'Bike', 'Truck', 'EV'],
                datasets: [{
                    data: [carSlots, bikeSlots, truckSlots, evSlots],
                    backgroundColor: ['#4f46e5', '#10b981', '#f59e0b', '#14b8a6'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'right',
                        labels: { color: textColor, boxWidth: 12, font: { size: 10 } }
                    }
                }
            }
        });
    }

    if (priceCtx) {
        charts.price = new Chart(priceCtx, {
            type: 'line',
            data: {
                labels: ['09:00', '11:00', '13:00', '15:00', '17:00', '19:00', '21:00'],
                datasets: [{
                    label: 'Dynamic Rate (₹/hr)',
                    data: priceData,
                    borderColor: '#fbbf24',
                    borderDash: [5, 5],
                    fill: false,
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { grid: { color: gridColor }, ticks: { color: textColor } },
                    y: { grid: { color: gridColor }, ticks: { color: textColor } }
                },
                plugins: { legend: { display: false } }
            }
        });
    }
}

let lastFetchedPlate = '';

async function handlePlateChange() {
    const plate = document.getElementById('input-plate').value.trim().toUpperCase();
    if (plate.length < 5 || plate === lastFetchedPlate) return;
    lastFetchedPlate = plate;

    if (useMockData) {
        const session = mockDb.sessions.find(s => s.vehicle.licensePlate === plate);
        if (session) {
            document.getElementById('select-type').value = session.vehicle.vehicleType.typeName;
            document.getElementById('input-owner').value = session.vehicle.ownerName || '';
            document.getElementById('input-contact').value = session.vehicle.ownerContact || '';
            showAnprFeedback(`✓ Loaded profile for vehicle: ${plate} (${session.vehicle.vehicleType.typeName})`, 'success');
        }
    } else {
        try {
            const res = await fetch(`${API_BASE}/vehicles/${plate}`);
            if (res.ok) {
                const vehicle = await res.json();
                if (vehicle) {
                    if (vehicle.vehicleType && vehicle.vehicleType.typeName) {
                        document.getElementById('select-type').value = vehicle.vehicleType.typeName;
                    }
                    document.getElementById('input-owner').value = vehicle.ownerName || '';
                    document.getElementById('input-contact').value = vehicle.ownerContact || '';
                    showAnprFeedback(`✓ Loaded profile for vehicle: ${plate} (${vehicle.vehicleType ? vehicle.vehicleType.typeName : 'CAR'})`, 'success');
                }
            }
        } catch (err) {
            console.error("Failed to query vehicle details:", err);
        }
    }
}

function showAnprFeedback(msg, type) {
    const feedbackDiv = document.getElementById('anpr-feedback');
    if (!feedbackDiv) return;
    const colors = {
        success: 'var(--neon-green)',
        warning: 'var(--neon-yellow)',
        error:   'var(--neon-red)'
    };
    feedbackDiv.style.color = colors[type] || 'var(--text-secondary)';
    feedbackDiv.textContent = msg;
    feedbackDiv.style.display = 'block';
    setTimeout(() => { feedbackDiv.style.display = 'none'; }, 6000);
}
