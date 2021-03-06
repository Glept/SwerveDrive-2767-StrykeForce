package org.strykeforce.thirdcoast.swerve

import com.ctre.phoenix.motorcontrol.SensorCollection
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import org.strykeforce.thirdcoast.util.Settings
import spock.lang.Specification

import static com.ctre.phoenix.motorcontrol.ControlMode.*
import static org.strykeforce.thirdcoast.swerve.SwerveDrive.DriveMode.TRAJECTORY

class WheelTest extends Specification {

    static final EPSILON = 1e-12d
    static final ROT = 4096d

    def azimuth = Mock(TalonSRX)
    def drive = Mock(TalonSRX)

    //
    // Settings
    //
    def "defaults are configured"() {
        when:
        def wheel = new DefaultWheel(new Settings(), azimuth, drive)

        then:
        with(wheel) {
            driveSetpointMax == 0
        }
    }


    def "override drive max setpoint"() {
        given:
        def toml = "[THIRDCOAST.WHEEL]\ndriveSetpointMax = 2767"

        when:
        def wheel = new DefaultWheel(new Settings(toml), azimuth, drive)

        then:
        with(wheel) {
            getDriveSetpointMax() == 2767
            // defaults
        }
    }

    //
    // Azimuth Zero
    //
    def "gets azimuth absolution position"() {
        setup:
        def sensorCollection = Mock(SensorCollection)
        azimuth.getSensorCollection() >> sensorCollection
        sensorCollection.getPulseWidthPosition() >> 0x1000

        when:
        def wheel = new DefaultWheel(new Settings(), azimuth, drive)

        then:
        wheel.azimuthAbsolutePosition == 0

        when:
        wheel.setAzimuthZero(2767)

        then:
        1 * azimuth.setSelectedSensorPosition(-2767, 0, 10)
    }

    //
    // Calculating Position and Output
    //
    def "azimuth changes are optimized"() {
        when:
        azimuth.getSelectedSensorPosition(0) >> start_position * ROT
        def wheel = new DefaultWheel(new Settings(), azimuth, drive)
        wheel.set(setpoint, 1d)

        then:
        1 * azimuth.set(MotionMagic, {
            Math.abs(it - end_position * ROT) < EPSILON
        })
        1 * drive.set(PercentOutput, is_reversed ? -1d : 1d)

        where:
        start_position | setpoint || end_position | is_reversed
        0              | 0        || 0            | false
        0              | 0.1      || -0.1         | false
        -0.1           | 0.2      || -0.2         | false
        -0.2           | 0.3      || -0.3         | false
        -0.3           | 0.4      || -0.4         | false
        -0.4           | 0.45     || -0.45        | false
        -0.45          | 0.5      || -0.5         | false
        -0.5           | 0.5      || -0.5         | false
        -0.5           | 0.45     || -0.45        | false
        -0.45          | -0.4     || -0.6         | false
        -0.6           | -0.3     || -0.7         | false
        -0.7           | -0.2     || -0.8         | false
        -0.8           | -0.1     || -0.9         | false
        -0.9           | -0.0     || -1.0         | false

        -0.1           | 0.1      || -0.1         | false
        0.1            | -0.1     || 0.1          | false

        -0.4           | 0.4      || -0.4         | false
        -0.6           | 0.3      || -0.8         | true  // true -> 1024
        -0.7           | 0.25     || -0.75        | true
        -0.75          | 0.1      || -0.6         | true
        -0.9           | -0.1     || -0.9         | false

        0              | 0.5      || 0            | true
        0              | -0.5     || 0            | true
        -0.5           | 0.5      || -0.5         | false

        -0.1           | -0.4     || -0.1         | true
        -0.1           | 0.4      || 0.1          | true
        -1.1           | -0.4     || -1.1         | true
        -1.1           | 0.4      || -0.9         | true

        2767.4         | -0.2     || 2767.2       | false
        -2767.4        | 0.2      || -2767.2      | false
        2767.4         | -10.2    || 2767.2       | false
        -2767.4        | 10.2     || -2767.2      | false

        0.25           | -0.25    || 0.25         | false
    }

    def "drive output is scaled"() {
        when:
        def tomlStr = "[THIRDCOAST.WHEEL]\ndriveSetpointMax=10_000"
        def wheel = new DefaultWheel(new Settings(tomlStr), azimuth, drive)
        wheel.setDriveMode(TRAJECTORY)
        wheel.set(0, setpoint)

        then:
        1 * drive.set(Velocity, output)

        where:
        setpoint || output
        1        || 10_000.0
        -1       || -10_000.0
        0        || 0
        0.5      || 5_000.0
        -0.5     || -5_000.0
    }


    def "neutral drive output leaves azimuths in previous position"() {
        when:
        def wheel = new DefaultWheel(new Settings(), azimuth, drive)
        wheel.set(0, 0)

        then:
        0 * azimuth.set(_)
        1 * drive.set(PercentOutput, 0)
    }
}
