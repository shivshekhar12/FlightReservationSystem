CREATE DATABASE IF NOT EXISTS flight_reservation;
USE flight_reservation;

create table Airline (
    airline_id char(2),
    name varchar(100),
    country varchar(100),
    primary key (airline_id)
);

create table Airport (
    airport_id char(3),
    name varchar(100),
    city varchar(100),
    country varchar(100),
    primary key (airport_id)
);

create table Airline_Airport (
    airline_id char(2),
    airport_id char(3),
    primary key (airline_id, airport_id),
    foreign key (airline_id) references Airline(airline_id),
    foreign key (airport_id) references Airport(airport_id)
);

create table Aircraft (
    aircraft_id integer,
    airline_id char(2),
    model varchar(100),
    economy_seats integer not null default 0,
    business_seats integer not null default 0,
    first_seats integer not null default 0,
    primary key (aircraft_id),
    foreign key (airline_id) references Airline(airline_id)
);

create table Flight (
    flight_no varchar(10),
    airline_id char(2),
    aircraft_id integer,
    dep_airport_id char(3),
    arr_airport_id char(3),
    dep_time time,
    arr_time time,
    is_international boolean,
    base_economy_fare decimal(10,2) not null default 0.00,
    base_business_fare decimal(10,2) not null default 0.00,
    base_first_fare decimal(10,2) not null default 0.00,
    booking_fee decimal(10,2) not null default 25.00,
    stops integer not null default 0,
    primary key (flight_no, airline_id),
    foreign key (airline_id) references Airline(airline_id),
    foreign key (aircraft_id) references Aircraft(aircraft_id),
    foreign key (dep_airport_id) references Airport(airport_id),
    foreign key (arr_airport_id) references Airport(airport_id)
);

create table Flight_Day (
    flight_no varchar(10),
    airline_id char(2),
    day_of_week varchar(10),
    primary key (flight_no, airline_id, day_of_week),
    foreign key (flight_no, airline_id) references Flight(flight_no, airline_id)
);

create table Customer (
    cust_id integer auto_increment,
    name varchar(100),
    email varchar(100),
    phone varchar(20),
    address varchar(200),
    passport_no varchar(50),
    username varchar(50) unique,
    password_hash varchar(255),
    primary key (cust_id)
);

create table Employee (
    emp_id integer auto_increment,
    name varchar(100),
    role enum('admin', 'customer_rep'),
    username varchar(50) unique,
    password_hash varchar(255),
    primary key (emp_id)
);

create table Ticket (
    ticket_no integer auto_increment,
    cust_id integer,
    handled_by integer,
    total_fare decimal(10,2),
    booking_fee decimal(10,2),
    trip_type enum('one_way', 'round_trip'),
    is_flexible boolean,
    purchase_date datetime default current_timestamp,
    status enum('active', 'cancelled', 'completed') not null default 'active',
    primary key (ticket_no),
    foreign key (cust_id) references Customer(cust_id),
    foreign key (handled_by) references Employee(emp_id)
);

create table Ticket_Flight (
    ticket_no integer,
    leg_order integer,
    flight_no varchar(10),
    airline_id char(2),
    dep_date date,
    seat_no varchar(10),
    class enum('economy', 'business', 'first'),
    meal_pref varchar(50),
    primary key (ticket_no, leg_order),
    foreign key (ticket_no) references Ticket(ticket_no),
    foreign key (flight_no, airline_id) references Flight(flight_no, airline_id)
);

create table Waitlist (
    cust_id integer,
    flight_no varchar(10),
    airline_id char(2),
    dep_date date,
    position integer,
    join_date datetime default current_timestamp,
    notified boolean not null default false,
    primary key (cust_id, flight_no, airline_id, dep_date),
    foreign key (cust_id) references Customer(cust_id),
    foreign key (flight_no, airline_id) references Flight(flight_no, airline_id)
);

create table Customer_Question (
    question_id integer auto_increment,
    cust_id integer,
    rep_id integer,
    subject varchar(200),
    question_text text not null,
    answer_text text,
    asked_at datetime default current_timestamp,
    answered_at datetime,
    primary key (question_id),
    foreign key (cust_id) references Customer(cust_id),
    foreign key (rep_id) references Employee(emp_id)
);

insert into Employee (name, role, username, password_hash)
values ('System Admin', 'admin', 'admin', 'admin123');

INSERT INTO Airline VALUES ('AA', 'American Airlines', 'USA');
INSERT INTO Airline VALUES ('UA', 'United Airlines', 'USA');

INSERT INTO Airport VALUES ('JFK', 'John F Kennedy', 'New York', 'USA');
INSERT INTO Airport VALUES ('LAX', 'Los Angeles Intl', 'Los Angeles', 'USA');
INSERT INTO Airport VALUES ('ORD', 'OHare Intl', 'Chicago', 'USA');

INSERT INTO Aircraft VALUES (1, 'AA', 'Boeing 737', 120, 30, 10);
INSERT INTO Aircraft VALUES (2, 'UA', 'Airbus A320', 100, 20, 8);

INSERT INTO Flight VALUES ('AA100','AA',1,'JFK','LAX','08:00','11:00',0,199.99,499.99,899.99,25.00,0);
INSERT INTO Flight VALUES ('UA200','UA',2,'JFK','ORD','09:00','11:00',0,149.99,399.99,699.99,25.00,0);

INSERT INTO Flight_Day VALUES ('AA100','AA','Monday');
INSERT INTO Flight_Day VALUES ('AA100','AA','Wednesday');
INSERT INTO Flight_Day VALUES ('AA100','AA','Friday');
INSERT INTO Flight_Day VALUES ('UA200','UA','Monday');
INSERT INTO Flight_Day VALUES ('UA200','UA','Tuesday');
INSERT INTO Flight_Day VALUES ('UA200','UA','Wednesday');
INSERT INTO Flight_Day VALUES ('UA200','UA','Thursday');
INSERT INTO Flight_Day VALUES ('UA200','UA','Friday');
INSERT INTO Flight_Day VALUES ('UA200','UA','Saturday');
INSERT INTO Flight_Day VALUES ('UA200','UA','Sunday');

INSERT INTO Customer (name,email,phone,username,password_hash)
VALUES ('John Doe','john@test.com','555-1234','john','password123');

INSERT INTO Employee (name,role,username,password_hash)
VALUES ('Jane Rep','customer_rep','jane','password123');