import { X, Check } from 'lucide-react';

export default function SeatMap({ train, onClose, onBook, bookingLoading }) {
  const seats = train.seats || [];
  
  // Calculate availability stats
  let totalSeats = 0;
  let bookedCount = 0;
  seats.forEach(row => {
    row.forEach(seat => {
      totalSeats++;
      if (seat === 1) bookedCount++;
    });
  });
  const availableCount = totalSeats - bookedCount;

  return (
    <div className="modal-backdrop">
      <div className="modal-content seat-map-modal card fade-in">
        <div className="modal-header">
          <div>
            <h3>Train {train.trainNo} - Coach Layout</h3>
            <p className="modal-subtitle">Visual Seat Map & Availability</p>
          </div>
          <button className="close-btn" onClick={onClose} disabled={bookingLoading}>
            <X size={20} />
          </button>
        </div>

        <div className="seat-map-body">
          {/* Legend */}
          <div className="seat-legend">
            <div className="legend-item">
              <span className="legend-box available"></span>
              <span>Available ({availableCount})</span>
            </div>
            <div className="legend-item">
              <span className="legend-box booked"></span>
              <span>Booked ({bookedCount})</span>
            </div>
          </div>

          {/* Coach graphic outline */}
          <div className="coach-container">
            <div className="coach-header-label">COACH S1 (Sleeper Class)</div>
            <div className="coach-grid">
              {seats.map((row, rIdx) => (
                <div key={rIdx} className="coach-row">
                  <div className="row-number-label">Row {rIdx + 1}</div>
                  <div className="row-seats">
                    {row.map((seat, sIdx) => {
                      const isBooked = seat === 1;
                      const seatName = `R${rIdx + 1} S${sIdx + 1}`;
                      return (
                        <div
                          key={sIdx}
                          className={`seat-box ${isBooked ? 'booked' : 'available'}`}
                          title={`${seatName} - ${isBooked ? 'Booked' : 'Available'}`}
                        >
                          <span className="seat-label">{sIdx + 1}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="booking-notice alert alert-success">
            <strong>System Auto-Allocation:</strong> Click the button below to book. The system will automatically reserve the first available seat (highlighted green) in real-time.
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose} disabled={bookingLoading}>
            Cancel
          </button>
          <button className="btn btn-primary" onClick={onBook} disabled={bookingLoading || availableCount === 0}>
            {bookingLoading ? (
              <span className="spinner"></span>
            ) : (
              <>
                <Check size={18} /> Book Ticket Now
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
