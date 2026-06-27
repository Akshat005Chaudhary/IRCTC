export const api = {
  // Helper to fetch the token dynamically and configure headers
  getHeaders() {
    const token = localStorage.getItem('token');
    const headers = {
      'Content-Type': 'application/json',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  },

  // Centralized request wrapper
  async request(endpoint, options = {}) {
    const response = await fetch(endpoint, {
      ...options,
      headers: {
        ...this.getHeaders(),
        ...options.headers,
      },
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `Request failed with status ${response.status}`);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }
    return response.text();
  },

  // Auth endpoints
  signup(user) {
    return this.request('/api/users/signup', {
      method: 'POST',
      body: JSON.stringify(user),
    });
  },

  login(email, password) {
    return this.request('/api/users/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  // Train endpoints
  searchTrains(source, destination) {
    return this.request(`/api/trains/search?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}`, {
      method: 'GET',
    });
  },

  // Booking & Ticket endpoints
  bookTicket(trainId, source, destination, dateOfTravel) {
    return this.request('/api/tickets/book', {
      method: 'POST',
      body: JSON.stringify({ trainId, source, destination, dateOfTravel }),
    });
  },

  fetchBookings() {
    return this.request('/api/tickets/bookings', {
      method: 'GET',
    });
  },

  cancelTicket(pnr) {
    return this.request('/api/tickets/cancel', {
      method: 'POST',
      body: JSON.stringify({ pnr }),
    });
  },
};
