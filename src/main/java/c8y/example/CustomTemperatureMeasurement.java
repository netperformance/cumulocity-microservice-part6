package c8y.example;
import java.math.BigDecimal;

import org.svenson.AbstractDynamicProperties;

import com.cumulocity.model.measurement.MeasurementValue;

public class CustomTemperatureMeasurement extends AbstractDynamicProperties {

	private static final long serialVersionUID = 1L;
	
	public static final String UNIT = "C";
	
	private MeasurementValue temperatureOutside = new MeasurementValue(UNIT);
	
	public MeasurementValue getTemperatureOutside() {
		return temperatureOutside;
	}
	
	public void setTemperatureOutside(MeasurementValue temperatureOutside) {
		this.temperatureOutside = temperatureOutside;
	}
	
	// Get the temperature (value)
	public BigDecimal getTemperature() {
		return temperatureOutside.getValue();
	}
	
	// Set the temperature (value)
	public void setTemperature(BigDecimal temperature) {
		temperatureOutside.setValue(temperature);
	}
	
}
