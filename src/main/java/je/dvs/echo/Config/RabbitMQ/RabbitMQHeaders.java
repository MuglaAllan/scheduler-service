package je.dvs.echo.config.rabbitmq;

public class RabbitMQHeaders {

    public enum Header {REQUEST,RESPONSE}

    public enum Name {
        SubmitApplicationData,
        CalculateRegistrationFee,
        AppointmentType,
        SubmitVehicle,
        SubmitPerson,
        GetStartingIndex,
        ReturnBookedAppointments
    }
}
