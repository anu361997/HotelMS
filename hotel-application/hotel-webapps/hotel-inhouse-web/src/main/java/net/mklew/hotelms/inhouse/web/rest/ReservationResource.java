package net.mklew.hotelms.inhouse.web.rest;

import com.google.common.base.Optional;
import com.sun.jersey.spi.resource.Singleton;
import net.mklew.hotelms.domain.booking.GuestRepository;
import net.mklew.hotelms.domain.booking.Id;
import net.mklew.hotelms.domain.booking.reservation.*;
import net.mklew.hotelms.domain.booking.reservation.rates.Rate;
import net.mklew.hotelms.domain.booking.reservation.rates.RateRepository;
import net.mklew.hotelms.domain.guests.Guest;
import net.mklew.hotelms.domain.room.Room;
import net.mklew.hotelms.domain.room.RoomName;
import net.mklew.hotelms.domain.room.RoomNotFoundException;
import net.mklew.hotelms.domain.room.RoomRepository;
import net.mklew.hotelms.inhouse.web.dto.ErrorDto;
import net.mklew.hotelms.inhouse.web.dto.GuestDto;
import net.mklew.hotelms.inhouse.web.dto.MissingGuestInformation;
import net.mklew.hotelms.inhouse.web.dto.ReservationDto;
import net.mklew.hotelms.inhouse.web.dto.dates.DateParser;
import net.mklew.hotelms.persistance.hibernate.configuration.HibernateSessionFactory;
import org.hibernate.Session;
import org.jcontainer.dna.Logger;

import javax.naming.OperationNotSupportedException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 12/24/12
 *        time 3:46 PM
 */
@Singleton
@Path("/reservations")
public class ReservationResource
{
    private final Logger logger;
    private final ReservationFactory reservationFactory;
    private final GuestRepository guestRepository;
    private final HibernateSessionFactory hibernateSessionFactory;
    private final RoomRepository roomRepository;
    private final RateRepository rateRepository;
    private final BookingService bookingService;
    private final ReservationRepository reservationRepository;
    private final CheckInService checkInService;
    private final CheckOutService checkOutService;
    private final CancellationService cancellationService;

    public ReservationResource(Logger logger, ReservationFactory reservationFactory, GuestRepository guestRepository,
                               HibernateSessionFactory hibernateSessionFactory, RoomRepository roomRepository,
                               RateRepository rateRepository, BookingService bookingService,
                               ReservationRepository reservationRepository, CheckInService checkInService,
                               CheckOutService checkOutService, CancellationService cancellationService)
    {
        this.logger = logger;
        this.reservationFactory = reservationFactory;
        this.guestRepository = guestRepository;
        this.hibernateSessionFactory = hibernateSessionFactory;
        this.roomRepository = roomRepository;
        this.rateRepository = rateRepository;
        this.bookingService = bookingService;
        this.reservationRepository = reservationRepository;
        this.checkInService = checkInService;
        this.checkOutService = checkOutService;
        this.cancellationService = cancellationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<ReservationDto> getAllReservations()
    {
        Session session = hibernateSessionFactory.getCurrentSession();
        session.beginTransaction();
        final Collection<Reservation> reservations = reservationRepository.getAll();
        Collection<ReservationDto> dtos = new ArrayList<>(reservations.size());
        for (Reservation reservation : reservations)
        {
            dtos.add(ReservationDto.fromReservation(reservation));
        }
        session.getTransaction().commit();
        return dtos;
    }

    @GET
    @Path("/{id}")
    public Response getReservation(@PathParam("id") String reservationId)
    {
        try
        {
            Id id = Id.of(reservationId);
            Session session = hibernateSessionFactory.getCurrentSession();
            session.beginTransaction();
            final Optional<Reservation> reservationOptional = reservationRepository.lookup(id);
            if (reservationOptional.isPresent())
            {
                final ReservationDto dto = ReservationDto.fromReservation(reservationOptional.get());
                session.getTransaction().commit();
                return Response.ok(dto, MediaType.APPLICATION_JSON_TYPE).status(Response.Status.OK).build();
            }
            else
            {
                session.getTransaction().commit();
                return Response.ok().status(Response.Status.NOT_FOUND).build();
            }

        }
        catch (Exception e)
        {
            throw e;
            //return Response.ok(e.getMessage()).build();
        }

    }

    @Path("/{id}/checkIn")
    @POST
    public Response checkInReservation(@PathParam("id") String reservationId)
    {
        Id id = Id.of(reservationId);
        Session session = hibernateSessionFactory.getCurrentSession();
        session.beginTransaction();
        final Optional<Reservation> reservationOptional = reservationRepository.lookup(id);
        if (reservationOptional.isPresent())
        {
            final Reservation reservation = reservationOptional.get();
            checkInService.checkIn(reservation);
            session.saveOrUpdate(reservation);
            session.getTransaction().commit();
            return Response.ok().status(Response.Status.OK).build();
        }
        else
        {
            session.getTransaction().commit();
            return Response.ok(new ErrorDto("Reservation with id " + reservationId + " has not been found.",
                    "RESERVATION-NOT-FOUND"), MediaType.APPLICATION_JSON_TYPE).status(Response.Status.BAD_REQUEST)
                    .build();
        }
    }

    @Path("/{id}/checkOut")
    @POST
    public Response checkOutReservation(@PathParam("id") String reservationId)
    {
        // TODO do actual checkOut
        return Response.ok("checkOut worked, reservationId was " + reservationId).status(Response.Status.OK).build();
    }

    @Path("/{id}/cancel")
    @POST
    public Response cancelReservation(@PathParam("id") String reservationId)
    {
        // TODO do actual checkOut
        return Response.ok("Cancel worked, reservationId was " + reservationId).status(Response.Status.OK).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createNewReservation(MultivaluedMap<String, String> formParams,
                                         @Context HttpServletResponse httpServletResponse)
    {
        Session session = hibernateSessionFactory.getCurrentSession();
        session.beginTransaction();
        logger.debug("Got new reservation with parameters: " + formParams.toString());
        try
        {
            GuestDto reservationOwner = GuestDto.fromReservationForm(formParams);
            ReservationDto reservationDto = ReservationDto.fromReservationForm(formParams);

            Guest owner;
            if (reservationOwner.exists())
            {
                owner = guestRepository.findGuestById(Long.parseLong(reservationOwner.id));
            }
            else
            {
                owner = new Guest(reservationOwner.socialTitle, reservationOwner.firstName,
                        reservationOwner.surname, reservationOwner.gender, reservationOwner.idType,
                        reservationOwner.idNumber, reservationOwner.phoneNumber);
                owner.setPreferences(reservationOwner.preferences);
                owner.setDateOfBirth(reservationOwner.dateOfBirthDate);
                // owner.setEmailAddress(); // todo add field to form, dto, and set it here
                // todo nationality, address and other
                guestRepository.saveGuest(owner);
            }

            // get room
            RoomName roomName = RoomName.getNameWithoutPrefix(reservationDto.getRoomName());
            final Room room = roomRepository.getRoomByName(roomName);
            // find rate
            Collection<Rate> rates = rateRepository.getAllRatesForRoom(room);
            Rate rate = getChosenRate(reservationDto, rates);

            // create reservation using factory
            Reservation reservation;
            if (ReservationType.fromName(reservationDto.getReservationType()).equals(ReservationType.SINGLE))
            {
                reservation = reservationFactory.createSingleReservation(owner, room, rate,
                        reservationDto.getCheckinDate(),
                        reservationDto.getCheckoutDate(), Integer.parseInt(reservationDto.getNumberOfAdults()),
                        Integer.parseInt(reservationDto.getNumberOfChildren()), Integer.parseInt(reservationDto
                        .getRoomExtraBed()));
            }
            else
            {
                throw new OperationNotSupportedException("Other reservation types are not supported ");
            }

            // book reservation or fail on exception
            bookingService.bookReservation(reservation);
            ReservationDto bookedDto = ReservationDto.fromReservation(reservation);

            session.getTransaction().commit();

            return Response.ok(bookedDto, MediaType.APPLICATION_JSON_TYPE).status(Response.Status.CREATED).build();
        }
        catch (MissingGuestInformation missingGuestInformation)
        {
            logger.error("Reservation owner has no sufficient information", missingGuestInformation);
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            // todo return response, setting status does not work
            return null;
        }
        catch (RoomNotFoundException e)
        {
            logger.error("Room not found exception", e);
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            // todo return response, setting status does not work
            return null;
        }
        catch (OperationNotSupportedException e)
        {
            logger.error("Operation not supported", e);
            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            // todo return response, setting status does not work
            return null;
        }
        catch (RoomIsUnavailableException e)
        {
            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            final String message = "Room " + e.getRoomName() + " is unavailable between " +
                    DateParser.fromDate(e.getCheckIn()) + " and " + DateParser.fromDate(e.getCheckOut());
            return Response.ok(new ErrorDto(message, "ROOM-UNAVAILABLE"), MediaType.APPLICATION_JSON_TYPE).status
                    (Response.Status.FORBIDDEN).build();
        }
    }

    private Rate getChosenRate(ReservationDto reservationDto, Collection<Rate> rates)
    {
        for (Rate rate : rates)
        {
            if (reservationDto.getRateType().equals(rate.getRateName()))
            {
                return rate;
            }
        }
        throw new RuntimeException("Rate not found");
    }
}
