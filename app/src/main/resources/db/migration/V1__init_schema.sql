CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(255),
    password VARCHAR(255) NOT NULL
);

CREATE TABLE user_tickets_booked (
    user_id VARCHAR(255) REFERENCES users(user_id) ON DELETE CASCADE,
    ticket_pnr VARCHAR(255),
    PRIMARY KEY (user_id, ticket_pnr)
);

CREATE TABLE trains (
    train_id VARCHAR(255) PRIMARY KEY,
    train_no VARCHAR(255) NOT NULL,
    seats TEXT NOT NULL
);

CREATE TABLE train_stations (
    train_id VARCHAR(255) REFERENCES trains(train_id) ON DELETE CASCADE,
    station_name VARCHAR(255),
    station_order INT,
    PRIMARY KEY (train_id, station_order)
);

CREATE TABLE train_station_times (
    train_id VARCHAR(255) REFERENCES trains(train_id) ON DELETE CASCADE,
    station_name VARCHAR(255),
    arrival_departure_time VARCHAR(255),
    PRIMARY KEY (train_id, station_name)
);

CREATE TABLE tickets (
    ticket_id VARCHAR(255) PRIMARY KEY,
    pnr VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) REFERENCES users(user_id) ON DELETE CASCADE,
    train_id VARCHAR(255) REFERENCES trains(train_id) ON DELETE CASCADE,
    source VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    date_of_travel VARCHAR(255) NOT NULL,
    seat_no VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL
);
