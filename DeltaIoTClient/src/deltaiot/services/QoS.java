
package deltaiot.services;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for qoS complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="qoS">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="energyConsumption" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="packetLoss" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="period" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "qoS", propOrder = {
    "energyConsumption",
    "packetLoss",
    "period"
})
public class QoS {

    protected double energyConsumption;
    protected double packetLoss;
    protected String period;

    /**
     * Gets the value of the energyConsumption property.
     * 
     */
    public double getEnergyConsumption() {
        return energyConsumption;
    }

    /**
     * Sets the value of the energyConsumption property.
     * 
     */
    public void setEnergyConsumption(double value) {
        this.energyConsumption = value;
    }

    /**
     * Gets the value of the packetLoss property.
     * 
     */
    public double getPacketLoss() {
        return packetLoss;
    }

    /**
     * Sets the value of the packetLoss property.
     * 
     */
    public void setPacketLoss(double value) {
        this.packetLoss = value;
    }

    /**
     * Gets the value of the period property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPeriod() {
        return period;
    }

    /**
     * Sets the value of the period property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPeriod(String value) {
        this.period = value;
    }

}
