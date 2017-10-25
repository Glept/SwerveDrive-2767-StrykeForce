package org.strykeforce.thirdcoast.telemetry.item;

import com.ctre.CANTalon;
import com.ctre.CANTalon.TalonControlMode;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.DoubleSupplier;
import org.strykeforce.thirdcoast.telemetry.grapher.Measure;

/**
 * Represents a {@link CANTalon} telemetry-enable Item.
 */
public class TalonItem extends AbstractItem {

  public final static String TYPE = "talon";
  public final static Set<Measure> MEASURES = Collections.unmodifiableSet(EnumSet.of(
      Measure.SETPOINT,
      Measure.OUTPUT_CURRENT,
      Measure.OUTPUT_VOLTAGE,
      Measure.ENCODER_POSITION,
      Measure.ENCODER_VELOCITY,
      Measure.ABSOLUTE_ENCODER_POSITION,
      Measure.CONTROL_LOOP_ERROR,
      Measure.INTEGRATOR_ACCUMULATOR,
      Measure.BUS_VOLTAGE,
      Measure.FORWARD_HARD_LIMIT_CLOSED,
      Measure.REVERSE_HARD_LIMIT_CLOSED,
      Measure.FORWARD_SOFT_LIMIT_OK,
      Measure.REVERSE_SOFT_LIMIT_OK
  ));
  // TODO: getMotionProfileStatus
  private final static String NA = "not available in API";
  private final CANTalon talon;
  private final Set<TalonControlMode> CLOSED_LOOP = EnumSet.of(
      TalonControlMode.Current,
      TalonControlMode.Position,
      TalonControlMode.Speed
  );

  public TalonItem(final CANTalon talon) {
    super(TYPE, talon.getDescription(), MEASURES);
    this.talon = talon;
  }

  public CANTalon getTalon() {
    return talon;
  }

  @Override
  public int id() {
    return talon.getDeviceID();
  }

  @Override
  public DoubleSupplier measurementFor(final Measure measure) {
    if (!MEASURES.contains(measure)) {
      throw new IllegalArgumentException("invalid measure: " + measure.name());
    }

    switch (measure) {
      case SETPOINT:
        return () -> talon.get();
      case OUTPUT_CURRENT:
        return () -> talon.getOutputCurrent();
      case OUTPUT_VOLTAGE:
        return () -> talon.getOutputVoltage();
      case ENCODER_POSITION:
        return () -> talon.getEncPosition();
      case ENCODER_VELOCITY:
        return () -> talon.getEncVelocity();
      case ABSOLUTE_ENCODER_POSITION:
        return () -> talon.getPulseWidthPosition();
      case CONTROL_LOOP_ERROR:
        return () -> talon.getClosedLoopError();
      case INTEGRATOR_ACCUMULATOR:
        return () -> talon.GetIaccum();
      case BUS_VOLTAGE:
        return () -> talon.getBusVoltage();
      case FORWARD_HARD_LIMIT_CLOSED:
        return () -> talon.isFwdLimitSwitchClosed() ? 1.0 : 0.0;
      case REVERSE_HARD_LIMIT_CLOSED:
        return () -> talon.isRevLimitSwitchClosed() ? 1.0 : 0.0;
      case FORWARD_SOFT_LIMIT_OK:
        // TODO: verify soft limit
        return () -> talon.getForwardSoftLimit();
      case REVERSE_SOFT_LIMIT_OK:
        return () -> talon.getReverseSoftLimit();
    }
    return () -> Double.NaN;

  }

  @Override
  public String toString() {
    return "TalonItem{" +
        "talon=" + talon +
        "} " + super.toString();
  }

  @Override
  public void toJson(JsonWriter writer) throws IOException {
    writer.beginObject();
    writer.name("type").value(TYPE);
    writer.name("deviceId").value(talon.getDeviceID());
    writer.name("description").value(talon.getDescription());
    writer.name("firmwareVersion").value(talon.GetFirmwareVersion());
    writer.name("controlMode").value(talon.getControlMode().toString());
    writer.name("brakeEnabledDuringNeutral").value(talon.getBrakeEnableDuringNeutral());
    writer.name("busVoltage").value(talon.getBusVoltage());
    writer.name("feedbackDevice").value(NA);
    writer.name("currentLimit").value(NA);
    writer.name("encoderCodesPerRef").value(NA);
    writer.name("inverted").value(talon.getInverted());
    writer.name("numberOfQuadIdxRises").value(talon.getNumberOfQuadIdxRises());
    writer.name("outputVoltage").value(talon.getOutputVoltage());
    writer.name("outputCurrent").value(talon.getOutputCurrent());

    writer.name("analogInput");
    writer.beginObject();
    writer.name("position").value(talon.getAnalogInPosition());
    writer.name("velocity").value(talon.getAnalogInVelocity());
    writer.name("raw").value(talon.getAnalogInRaw());
    writer.endObject();

    writer.name("encoder");
    writer.beginObject();
    writer.name("position").value(talon.getEncPosition());
    writer.name("velocity").value(talon.getEncVelocity());
    writer.endObject();

    writer.name("closedLoop");
    writer.beginObject();
    if (CLOSED_LOOP.contains(talon.getControlMode())) {
      writer.name("enabled").value(true);
      writer.name("p").value(talon.getP());
      writer.name("i").value(talon.getI());
      writer.name("d").value(talon.getD());
      writer.name("f").value(talon.getF());
      writer.name("iAccum").value(talon.GetIaccum());
      writer.name("iZone").value(talon.getIZone());
      writer.name("errorInt").value(talon.getClosedLoopError());
      writer.name("errorDouble").value(talon.getError());
      writer.name("rampRate").value(talon.getCloseLoopRampRate());
      writer.name("nominalVoltage").value(talon.GetNominalClosedLoopVoltage());
    } else {
      writer.name("enabled").value(false);
    }
    writer.endObject();

    writer.name("motionMagic");
    writer.beginObject();
    if (talon.getControlMode() == TalonControlMode.MotionMagic) {
      writer.name("enabled").value(true);
      writer.name("acceleration").value(talon.getMotionMagicAcceleration());
      writer.name("actTrajPosition").value(talon.getMotionMagicActTrajPosition());
      writer.name("actTrajVelocity").value(talon.getMotionMagicActTrajVelocity());
      writer.name("cruiseVelocity").value(talon.getMotionMagicCruiseVelocity());
    } else {
      writer.name("enabled").value(false);
    }
    writer.endObject();

    writer.name("motionProfile");
    writer.beginObject();
    if (talon.getControlMode() == TalonControlMode.MotionProfile) {
      writer.name("enabled").value(true);
      writer.name("topLevelBufferCount").value(talon.getMotionProfileTopLevelBufferCount());
    } else {
      writer.name("enabled").value(false);
    }
    writer.endObject();

    writer.name("forwardSoftLimit");
    writer.beginObject();
    writer.name("enabled").value(talon.isForwardSoftLimitEnabled());
    if (talon.isForwardSoftLimitEnabled()) {
      writer.name("limit").value(talon.getForwardSoftLimit());
    }
    writer.endObject();

    writer.name("reverseSoftLimit");
    writer.beginObject();
    writer.name("enabled").value(talon.isReverseSoftLimitEnabled());
    if (talon.isReverseSoftLimitEnabled()) {
      writer.name("limit").value(talon.getReverseSoftLimit());
    }
    writer.endObject();

    writer.name("lastError").value(talon.getLastError());
    writer.name("faults");
    writer.beginObject();
    writer.name("lim").value(talon.getFaultForLim());
    writer.name("stickyLim").value(talon.getStickyFaultForLim());
    writer.name("softLim").value(talon.getFaultForSoftLim());
    writer.name("stickySoftLim").value(talon.getStickyFaultForSoftLim());
    writer.name("hardwareFailure").value(talon.getFaultHardwareFailure());
    writer.name("overTemp").value(talon.getFaultOverTemp());
    writer.name("stickyOverTemp").value(talon.getStickyFaultOverTemp());
    writer.name("revLim").value(talon.getFaultRevLim());
    writer.name("stickyRevLim").value(talon.getStickyFaultRevLim());
    writer.name("revSoftLim").value(talon.getFaultRevSoftLim());
    writer.name("stickyRevSoftLim").value(talon.getStickyFaultRevSoftLim());
    writer.name("underVoltage").value(talon.getFaultUnderVoltage());
    writer.name("stickyUnderVoltage").value(talon.getStickyFaultUnderVoltage());
    writer.endObject();

    writer.endObject();
  }

}
