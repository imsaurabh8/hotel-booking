# HMS - Hotel Booking Backend

Spring Boot 3 / Java 17+ service for onboarding properties, searching, booking, paying and cancelling.
Data lives in memory behind repository interfaces; the payment provider is mocked behind an interface.

## Run

```bash
mvn test                  # 45 tests (unit tests + one end-to-end HTTP test)
mvn spring-boot:run       # http://localhost:8080
```

## API

| Method | Path | What it does |
|---|---|---|
| POST | `/api/owners` | Register an owner, optionally with properties (one = standalone host, many = chain) |
| POST | `/api/owners/{id}/properties` | Add a property to an owner |
| GET | `/api/owners/{id}`, `/api/properties/{id}` | Read |
| GET | `/api/properties/search` | Search: `checkIn`, `checkOut`, `guests`, `city`, `locality`, `minPrice`, `maxPrice`, `amenities` (comma list), `minStars` |
| POST | `/api/bookings` | Book a room type for a date range (`PENDING_PAYMENT`, room is held) |
| GET | `/api/bookings/{id}` | Read a booking |
| POST | `/api/bookings/{id}/payment` | Pay with `CARD`, `UPI` or `WALLET` (200 confirmed, 402 declined) |
| POST | `/api/bookings/{id}/cancel` | Cancel, refund per policy |

```bash
curl -X POST localhost:8080/api/owners -H 'Content-Type: application/json' -d '{
  "name":"Asha","properties":[{"name":"Sea View","city":"Mumbai","locality":"Bandra","stars":4,
  "amenities":["wifi","pool"],"roomTypes":[{"name":"Deluxe","capacity":2,"totalRooms":2,"pricePerNight":5000}]}]}'

curl 'localhost:8080/api/properties/search?city=Mumbai&checkIn=2026-12-01&checkOut=2026-12-03&guests=2&amenities=pool'

curl -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{
  "propertyId":"<id>","roomTypeId":"<id>","checkIn":"2026-12-01","checkOut":"2026-12-03",
  "guests":2,"guestName":"Ravi","guestEmail":"ravi@example.com"}'

curl -X POST localhost:8080/api/bookings/<id>/payment -H 'Content-Type: application/json' \
  -d '{"method":"CARD","details":{"cardNumber":"4111111111111111","cvv":"123"}}'

curl -X POST localhost:8080/api/bookings/<id>/cancel
```

Mock provider rule: any card number, UPI id or wallet id that ends with `0002` or starts with `fail` is declined.
Details per method: CARD `cardNumber`, `cvv`; UPI `vpa`; WALLET `walletId`.
Errors come back as 400 (bad input), 404 (unknown id) or 409 (sold out / wrong booking state).

## Structure

```
model/         Owner, Property, RoomType, Booking, BookingStatus
repository/    interfaces + in-memory implementations
service/       PropertyService, BookingService, AvailabilityService
search/        SearchService + SearchFilter implementations
payment/       PaymentMethod (card, UPI, wallet), PaymentGateway (mock provider), PaymentService
cancellation/  CancellationPolicy + tiered implementation
controller/    REST endpoints
```

## Design decisions

- **Single property vs chain:** an `Owner` has any number of `Property`s. A standalone host is just an owner with one, so there is one code path.
- **Availability:** a room type has `totalRooms`. Free rooms = total minus bookings that hold a room, counted night by night. Only `PENDING_PAYMENT` and `CONFIRMED` bookings hold a room, so a failed payment or a cancellation frees the room with no extra bookkeeping. Check-out day is free for the next guest.
- **No double-booking:** the availability check and the save happen together inside a lock per room type.
- **Booking states:** `PENDING_PAYMENT -> CONFIRMED | PAYMENT_FAILED | CANCELLED`, `CONFIRMED -> CANCELLED`. The allowed moves live in `BookingStatus`; any other move is a 409.
- **Payment drives the booking:** success confirms it, a decline marks it `PAYMENT_FAILED` and frees the room. Payments on one booking are serialised, so a double click charges once.
- **Extensibility (add a class, change nothing else):**
  - new payment method: implement `PaymentMethod` as a bean;
  - new search filter: implement `SearchFilter` as a bean (it reads its own request params);
  - new refund rules: implement `CancellationPolicy`.
  Tests add a payment method and a filter this way.
- **Payment provider:** each `PaymentMethod` only validates its own details, then charges through the `PaymentGateway` interface. `MockPaymentGateway` is the only implementation; a real provider replaces that one class.
- **Cancellation:** the refund goes through the gateway before the booking is marked cancelled, so a failed refund leaves it untouched. The default policy refunds 100% at 7+ days before check-in, 50% at 2-6 days, nothing closer.

## Assumptions

- One booking is one room; guests must not exceed the room type capacity.
- Stays are 1-30 nights, check-in not in the past. A single currency.
- A failed payment is final; the guest books again.
- Cancelling is refused after the stay is over.
- Locks are per JVM, which is fine for in-memory storage. No auth (out of scope).

## With more time

- A real database with per-night inventory rows and optimistic locking so it works across instances.
- Release `PENDING_PAYMENT` bookings automatically after a timeout.
- Request validation annotations, pagination for search, per-property cancellation policies.
