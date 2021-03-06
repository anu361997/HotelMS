package net.mklew.hotelms.domain.booking.reservation;

import com.google.common.base.Optional;
import net.mklew.hotelms.domain.booking.Id;
import net.mklew.hotelms.domain.booking.ReservationStatus;
import org.joda.time.DateTime;
import org.joda.time.base.BaseDateTime;

import java.util.Collection;

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 12/25/12
 *        time 9:14 PM
 */
public interface ReservationRepository
{
    /**
     * Returns reservations which overlap given interval
     *
     * @param checkIn
     * @param checkOut
     * @return
     */
    Collection<Reservation> findAllReservationsAroundDates(DateTime checkIn, DateTime checkOut);

    Collection<Reservation> findReservedForNextDay();

    void bookNewReservation(Reservation reservation);

    Collection<Reservation> getAll();

    Optional<Reservation> lookup(Id id);

    void deleteReservation(Reservation reservation);

    void update(Reservation reservation);

    void updateAll(Collection<Reservation> reservations);

    Collection<Reservation> findWithStatus(ReservationStatus status);
}
