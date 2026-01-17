
# CineSync - Concurrent Movie Booking System


A robust, scalable movie booking system built with Spring Boot that handles concurrent seat reservations efficiently. This system ensures data consistency and prevents double bookings even when multiple users attempt to book the same seat simultaneously.

## 🎯 Features

- **Concurrent Booking Handling**: Implements pessimistic locking and optimistic concurrency control to handle simultaneous bookings
- **User Management**: Complete user registration and profile management
- **Movie Catalog**: Add, search, and filter movies by genre and title
- **Show Management**: Create and manage movie shows with automatic seat generation
- **Seat Reservation**: Real-time seat availability checking and booking
- **Booking Management**: Create, view, and cancel bookings with automatic seat release
- **RESTful API**: Well-structured REST endpoints with proper error handling
- **Transaction Safety**: ACID-compliant transactions ensuring data integrity

## 🛠️ Tech Stack

- **Backend Framework**: Spring Boot 3.5.3
- **Language**: Java 17
- **Database**: MySQL 8.0
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven
- **API Documentation**: Postman Collection

## 📋 Prerequisites

Before running this project, ensure you have the following installed:

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <your-repository-url>
cd "demo 10"
```

### 2. Database Setup

Create the MySQL database:

```sql
CREATE DATABASE movie_booking_db;
```

### 3. Configure Database Connection

Update `src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/movie_booking_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Endpoints

#### Users
- `POST /api/users` - Create a new user
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email

#### Movies
- `POST /api/movies` - Create a new movie
- `GET /api/movies` - Get all movies
- `GET /api/movies/{id}` - Get movie by ID
- `GET /api/movies/search?title={title}` - Search movies by title
- `GET /api/movies/genre/{genre}` - Get movies by genre

#### Shows
- `POST /api/shows/movie/{movieId}` - Create a show for a movie
- `GET /api/shows/movie/{movieId}` - Get all shows for a movie
- `GET /api/shows/movie/{movieId}/upcoming` - Get upcoming shows
- `GET /api/shows/{id}` - Get show by ID

#### Seats
- `GET /api/seats/show/{showId}` - Get all seats for a show
- `GET /api/seats/show/{showId}/available` - Get available seats

#### Bookings
- `POST /api/bookings` - Create a booking (handles concurrency)
- `GET /api/bookings/{id}` - Get booking by ID
- `GET /api/bookings/user/{userId}` - Get user's bookings
- `PUT /api/bookings/{id}/cancel` - Cancel a booking

### Postman Collection

Import the `Movie_Booking_System.postman_collection.json` file into Postman to test all endpoints with pre-configured requests.

## 🔒 Concurrency Handling

This system implements multiple strategies to handle concurrent bookings:

### 1. Pessimistic Locking
- Uses `PESSIMISTIC_WRITE` lock on seats during booking
- Prevents other transactions from modifying seats simultaneously
- Ensures only one booking succeeds for the same seat

### 2. Optimistic Locking
- Uses `@Version` annotation on `Seat` and `Booking` entities
- Detects concurrent modifications through version numbers
- Automatically retries failed transactions (up to 3 attempts)

### 3. Retry Mechanism
- Exponential backoff strategy for retry attempts
- Handles `ObjectOptimisticLockingFailureException`
- Provides clear error messages to users

### 4. Transaction Management
- All booking operations are wrapped in `@Transactional`
- Ensures ACID properties (Atomicity, Consistency, Isolation, Durability)
- Automatic rollback on failures

## 📁 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/demo/
│   │       ├── controller/     # REST controllers
│   │       ├── service/        # Business logic layer
│   │       ├── repository/     # Data access layer
│   │       ├── model/          # Entity classes
│   │       ├── dto/            # Data Transfer Objects
│   │       └── exception/      # Exception handlers
│   └── resources/
│       └── application.properties
└── test/
    └── java/
```

## 🧪 Testing the Concurrency Feature

### Using Postman

1. Create a show with multiple seats
2. Create two booking requests with the same seat IDs
3. Run both requests simultaneously using Postman's "Run Collection" feature
4. Only one booking should succeed; the other will return a 409 Conflict error

### Example Request

```json
POST /api/bookings
{
  "userId": 1,
  "showId": 1,
  "seatIds": [1, 2, 3]
}
```

## 📊 Database Schema

- **users**: User information
- **movies**: Movie details
- **shows**: Show timings and screen information
- **seats**: Seat details with status and version
- **bookings**: Booking records
- **booking_seats**: Many-to-many relationship between bookings and seats

## 🎬 Example Workflow

1. **Create a User**
   ```bash
   POST /api/users
   {
     "email": "john@example.com",
     "name": "John Doe",
     "phoneNumber": "1234567890"
   }
   ```

2. **Create a Movie**
   ```bash
   POST /api/movies
   {
     "title": "The Matrix",
     "description": "A computer hacker learns about reality",
     "duration": 136,
     "genre": "Sci-Fi",
     "language": "English",
     "releaseDate": "1999-03-31",
     "ticketPrice": 500.0
   }
   ```

3. **Create a Show**
   ```bash
   POST /api/shows/movie/1
   {
     "showTime": "2025-12-15T18:00:00",
     "screenNumber": "Screen 1",
     "totalSeats": 50
   }
   ```

4. **Check Available Seats**
   ```bash
   GET /api/seats/show/1/available
   ```

5. **Create a Booking**
   ```bash
   POST /api/bookings
   {
     "userId": 1,
     "showId": 1,
     "seatIds": [1, 2, 3]
   }
   ```

## 🐛 Error Handling

The system includes comprehensive error handling:

- **404 Not Found**: Resource not found (user, movie, show, seat, booking)
- **409 Conflict**: Seat already booked or concurrent modification detected
- **400 Bad Request**: Invalid input data
- **500 Internal Server Error**: Unexpected server errors

All errors return a standardized JSON response:

```json
{
  "timestamp": "2025-12-13T13:30:00",
  "status": 409,
  "error": "Seat Already Booked",
  "message": "Seat A1 is already booked or reserved"
}
```

## 🔧 Configuration

### Application Properties

```properties
spring.application.name=movie-booking-system
spring.datasource.url=jdbc:mysql://localhost:3306/movie_booking_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 📝 Key Features Explained

### Automatic Seat Generation
When a show is created, seats are automatically generated based on `totalSeats`. Seats are named using a grid pattern (A1, A2, B1, B2, etc.).

### Seat Status Management
- **AVAILABLE**: Seat is free and can be booked
- **BOOKED**: Seat has been booked
- **RESERVED**: Seat is temporarily reserved (for future use)

### Booking Cancellation
When a booking is cancelled, all associated seats are automatically released and marked as AVAILABLE.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the MIT License.

## 🎯 Future Enhancements

- [ ] Add authentication and authorization (JWT)
- [ ] Implement payment gateway integration
- [ ] Add email notifications for bookings
- [ ] Implement seat reservation timeout
- [ ] Add admin dashboard
- [ ] Implement caching for better performance
- [ ] Add unit and integration tests
- [ ] Docker containerization
- [ ] API documentation with Swagger/OpenAPI

---


<<<<<<< HEAD

=======
>>>>>>> 32a48613eccdf43c0ec1ef5a7f174be977893694
