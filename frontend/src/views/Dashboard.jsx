import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { Search, Ticket, RefreshCw, Train, ArrowRight, User, LogOut } from 'lucide-react';
import SeatMap from './SeatMap';

const STATIONS = [
  'Delhi', 'Kanpur', 'Patna', 'Howrah',
  'Mumbai', 'Surat', 'Vadodara', 'Ahmedabad'
];

export default function Dashboard() {
  const { userEmail, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('search'); // 'search' or 'bookings'
  
  // Search Form State
  const [source, setSource] = useState('Delhi');
  const [destination, setDestination] = useState('Kanpur');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState('');

  // Bookings State
  const [bookings, setBookings] = useState([]);
  const [bookingsLoading, setBookingsLoading] = useState(false);
  const [bookingsError, setBookingsError] = useState('');

  // Selected Train for Seat Map Modal
  const [selectedTrain, setSelectedTrain] = useState(null);
  const [bookingLoading, setBookingLoading] = useState(false);
  
  // Booking success confirmation state
  const [confirmedTicket, setConfirmedTicket] = useState(null);

  // Fetch Bookings
  const fetchUserBookings = async () => {
    setBookingsLoading(true);
    setBookingsError('');
    try {
      const data = await api.fetchBookings();
      setBookings(data);
    } catch (err) {
      setBookingsError(err.message || 'Failed to fetch bookings.');
    } finally {
      setBookingsLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'bookings') {
      fetchUserBookings();
    }
  }, [activeTab]);

  // Handle Train Search
  const handleSearch = async (e) => {
    e.preventDefault();
    if (source === destination) {
      setSearchError('Source and Destination cannot be the same.');
      setSearchResults([]);
      return;
    }

    setSearchLoading(true);
    setSearchError('');
    try {
      const trains = await api.searchTrains(source, destination);
      setSearchResults(trains);
      if (trains.length === 0) {
        setSearchError('No trains found for this route.');
      }
    } catch (err) {
      setSearchError(err.message || 'Error occurred while searching trains.');
      setSearchResults([]);
    } finally {
      setSearchLoading(false);
    }
  };

  // Handle booking action
  const handleBook = async () => {
    if (!selectedTrain) return;
    setBookingLoading(true);
    try {
      const ticket = await api.bookTicket(selectedTrain.trainId, source, destination, date);
      setConfirmedTicket(ticket);
      setSelectedTrain(null); // Close seat map modal
      // Refresh search results to show updated seats if user searches again
      const updatedTrains = await api.searchTrains(source, destination);
      setSearchResults(updatedTrains);
    } catch (err) {
      alert(err.message || 'Failed to book ticket.');
    } finally {
      setBookingLoading(false);
    }
  };

  // Handle Cancellation
  const handleCancel = async (pnr) => {
    if (!window.confirm(`Are you sure you want to cancel PNR: ${pnr}?`)) return;
    try {
      await api.cancelTicket(pnr);
      alert('Ticket cancelled successfully. Refund processed.');
      fetchUserBookings();
    } catch (err) {
      alert(err.message || 'Failed to cancel ticket.');
    }
  };

  return (
    <div className="dashboard-container fade-in">
      {/* Navigation */}
      <nav className="navbar">
        <div className="nav-brand">
          <Train className="nav-logo" size={24} />
          <span>RailConnect</span>
        </div>
        <div className="nav-tabs">
          <button 
            className={`nav-tab-btn ${activeTab === 'search' ? 'active' : ''}`}
            onClick={() => setActiveTab('search')}
          >
            <Search size={18} /> Search Trains
          </button>
          <button 
            className={`nav-tab-btn ${activeTab === 'bookings' ? 'active' : ''}`}
            onClick={() => setActiveTab('bookings')}
          >
            <Ticket size={18} /> My Bookings
          </button>
        </div>
        <div className="nav-user">
          <div className="user-profile">
            <User size={16} />
            <span>{userEmail}</span>
          </div>
          <button className="logout-btn" onClick={logout} title="Log Out">
            <LogOut size={18} />
          </button>
        </div>
      </nav>

      {/* Main Content Area */}
      <main className="dashboard-content">
        {activeTab === 'search' && (
          <div className="search-section">
            <div className="card search-card">
              <h2>Book Your Journey</h2>
              <form onSubmit={handleSearch} className="search-form">
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">From</label>
                    <select 
                      value={source} 
                      onChange={(e) => setSource(e.target.value)}
                      className="form-input"
                    >
                      {STATIONS.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                  
                  <div className="form-group">
                    <label className="form-label">To</label>
                    <select 
                      value={destination} 
                      onChange={(e) => setDestination(e.target.value)}
                      className="form-input"
                    >
                      {STATIONS.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Date of Travel</label>
                    <input 
                      type="date" 
                      value={date} 
                      onChange={(e) => setDate(e.target.value)}
                      min={new Date().toISOString().split('T')[0]}
                      className="form-input"
                      required
                    />
                  </div>
                </div>

                <button type="submit" className="btn btn-primary search-btn" disabled={searchLoading}>
                  {searchLoading ? <span className="spinner"></span> : <><Search size={18} /> Search Trains</>}
                </button>
              </form>
            </div>

            {/* Results Grid */}
            <div className="results-container">
              {searchError && <div className="alert alert-error">{searchError}</div>}
              
              {searchResults.length > 0 && (
                <div className="trains-list">
                  <h3>Available Trains ({searchResults.length})</h3>
                  {searchResults.map((train) => {
                    // Count available seats
                    let availableSeats = 0;
                    train.seats.forEach(r => r.forEach(s => { if(s === 0) availableSeats++; }));

                    return (
                      <div key={train.trainId} className="card train-card fade-in">
                        <div className="train-info-row">
                          <div className="train-name-no">
                            <h4>{train.trainNo} - Superfast Express</h4>
                            <span className="train-id-badge">{train.trainId}</span>
                          </div>
                          <div className="seats-available-badge">
                            <span className="seats-count">{availableSeats}</span> seats left
                          </div>
                        </div>

                        {/* Stations Timeline */}
                        <div className="stations-timeline">
                          {train.stations.map((station, idx) => {
                            const isSource = station === source;
                            const isDest = station === destination;
                            const isInRoute = train.stations.indexOf(station) >= train.stations.indexOf(source) && 
                                              train.stations.indexOf(station) <= train.stations.indexOf(destination);

                            return (
                              <div 
                                key={station} 
                                className={`timeline-node ${isSource ? 'node-source' : ''} ${isDest ? 'node-dest' : ''} ${isInRoute ? 'in-route' : 'out-route'}`}
                              >
                                <div className="node-dot"></div>
                                <span className="node-station">{station}</span>
                                <span className="node-time">{train.stationTimes[station] || '--:--'}</span>
                              </div>
                            );
                          })}
                        </div>

                        <div className="train-actions">
                          <button 
                            className="btn btn-primary" 
                            onClick={() => setSelectedTrain(train)}
                            disabled={availableSeats === 0}
                          >
                            View Seats & Book
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'bookings' && (
          <div className="bookings-section">
            <div className="bookings-header">
              <h2>Your Booking History</h2>
              <button className="btn btn-secondary btn-icon" onClick={fetchUserBookings} disabled={bookingsLoading}>
                <RefreshCw size={16} className={bookingsLoading ? 'spin-icon' : ''} /> Refresh
              </button>
            </div>

            {bookingsError && <div className="alert alert-error">{bookingsError}</div>}
            
            {bookingsLoading ? (
              <div className="loading-container">
                <span className="spinner big-spinner"></span>
                <p>Retrieving your bookings...</p>
              </div>
            ) : bookings.length === 0 ? (
              <div className="card empty-bookings">
                <Ticket size={48} className="empty-icon" />
                <h3>No Bookings Found</h3>
                <p>You haven't booked any tickets yet. Go to the search tab to find trains!</p>
                <button className="btn btn-primary" onClick={() => setActiveTab('search')}>
                  Book Now
                </button>
              </div>
            ) : (
              <div className="bookings-list">
                {bookings.map((booking) => (
                  <div key={booking.ticketId} className="card ticket-card fade-in">
                    <div className="ticket-body">
                      <div className="ticket-left">
                        <div className="ticket-pnr-section">
                          <span className="pnr-label">PNR NUMBER</span>
                          <span className="pnr-value">{booking.pnr}</span>
                        </div>
                        <div className="ticket-route">
                          <div className="route-stop">
                            <span className="stop-city">{booking.source}</span>
                          </div>
                          <ArrowRight className="route-arrow" />
                          <div className="route-stop">
                            <span className="stop-city">{booking.destination}</span>
                          </div>
                        </div>
                        <div className="ticket-meta-grid">
                          <div>
                            <span className="meta-label">TRAIN NO</span>
                            <span className="meta-value">{booking.trainId}</span>
                          </div>
                          <div>
                            <span className="meta-label">DATE</span>
                            <span className="meta-value">{booking.dateOfTravel}</span>
                          </div>
                          <div>
                            <span className="meta-label">SEAT NO</span>
                            <span className="meta-value">{booking.seatNo}</span>
                          </div>
                        </div>
                      </div>
                      <div className="ticket-right">
                        <div className="ticket-price">
                          <span className="price-label">TOTAL FARE</span>
                          <span className="price-value">{booking.price} {booking.currency}</span>
                        </div>
                        <button className="btn btn-outline cancel-btn" onClick={() => handleCancel(booking.pnr)}>
                          Cancel Ticket
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      {/* Seat Map Modal */}
      {selectedTrain && (
        <SeatMap 
          train={selectedTrain}
          onClose={() => setSelectedTrain(null)}
          onBook={handleBook}
          bookingLoading={bookingLoading}
        />
      )}

      {/* Booking Success Confirmation Modal */}
      {confirmedTicket && (
        <div className="modal-backdrop">
          <div className="modal-content ticket-success-modal card fade-in">
            <div className="success-banner">
              <div className="success-icon-circle">
                <CheckIcon size={32} />
              </div>
              <h3>Ticket Booked Successfully!</h3>
              <p>Your reservation has been confirmed.</p>
            </div>
            
            <div className="visual-ticket">
              <div className="visual-ticket-header">
                <div>
                  <span className="visual-ticket-title">BOARDING PASS</span>
                  <span className="visual-ticket-subtitle">RailConnect Express</span>
                </div>
                <div className="visual-ticket-pnr">
                  <span className="v-pnr-label">PNR</span>
                  <span className="v-pnr-val">{confirmedTicket.pnr}</span>
                </div>
              </div>
              
              <div className="visual-ticket-body">
                <div className="v-route-row">
                  <div>
                    <div className="v-city">{confirmedTicket.source}</div>
                    <div className="v-label">Source</div>
                  </div>
                  <ArrowRight size={20} className="v-arrow" />
                  <div>
                    <div className="v-city">{confirmedTicket.destination}</div>
                    <div className="v-label">Destination</div>
                  </div>
                </div>

                <div className="v-meta-row">
                  <div>
                    <div className="v-meta-val">{confirmedTicket.trainId}</div>
                    <div className="v-meta-lbl">Train</div>
                  </div>
                  <div>
                    <div className="v-meta-val">{confirmedTicket.dateOfTravel}</div>
                    <div className="v-meta-lbl">Date of Travel</div>
                  </div>
                  <div>
                    <div className="v-meta-val">{confirmedTicket.seatNo}</div>
                    <div className="v-meta-lbl">Seat allocated</div>
                  </div>
                </div>

                <div className="v-price-row">
                  <div>
                    <div className="v-meta-val">{confirmedTicket.price} {confirmedTicket.currency}</div>
                    <div className="v-meta-lbl">Fare Paid</div>
                  </div>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button 
                className="btn btn-primary btn-block" 
                onClick={() => {
                  setConfirmedTicket(null);
                  setActiveTab('bookings'); // Redirect to my bookings
                }}
              >
                Go to My Bookings
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// Simple internal check helper since Lucide react doesn't have CheckIcon, it has Check
function CheckIcon({ size }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-check">
      <path d="M20 6 9 17l-5-5"/>
    </svg>
  );
}
