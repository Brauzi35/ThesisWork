package antiFrag;

import deltaiot.DeltaIoTSimulator;
import domain.*;
import simulator.Simulator;

public class NetworkSimulatorAF extends DeltaIoTSimulator {

    final static int GATEWAY_ID = 1;


    public static Simulator createSimulatorForAF(){

        Simulator simul = new Simulator();
        //System.out.println("post simulator");
        // Motes
        int load = 10; //number of packets to send in a turn; default ws 10
        double battery = 11880.0; //default was 11880.0
        double posScale = 2; //default was 2
        Mote mote2  = new Mote(2 , battery, load, new Position(378 * posScale, 193 * posScale));
        Mote mote3  = new Mote(3 , battery, load, new Position(365 * posScale, 343 * posScale));
        Mote mote4  = new Mote(4 , battery, load, new Position(508 * posScale, 296 * posScale));
        Mote mote5  = new Mote(5 , battery, load, new Position(603 * posScale, 440 * posScale));
        Mote mote6  = new Mote(6 , battery, load, new Position(628 * posScale, 309 * posScale));
        Mote mote7  = new Mote(7 , battery, load, new Position(324 * posScale, 273 * posScale));
        Mote mote8  = new Mote(8 , battery, load, new Position(392 * posScale, 478 * posScale));
        Mote mote9  = new Mote(9 , battery, load, new Position(540 * posScale, 479 * posScale));
        Mote mote10 = new Mote(10, battery, load, new Position(694 * posScale, 356 * posScale));
        Mote mote11 = new Mote(11, battery, load, new Position(234 * posScale, 232 * posScale));
        Mote mote12 = new Mote(12, battery, load, new Position(221 * posScale, 322 * posScale));
        Mote mote13 = new Mote(13, battery, load, new Position(142 * posScale, 170 * posScale));
        Mote mote14 = new Mote(14, battery, load, new Position(139 * posScale, 293 * posScale));
        Mote mote15 = new Mote(15, battery, load, new Position(128 * posScale, 344 * posScale));

        Mote[] allMotes = new Mote[]{mote2, mote3, mote4, mote5, mote6, mote7, mote8, mote9, mote10, mote11, mote12, mote13, mote14, mote15};
        simul.addMotes(allMotes); //ignore the first null elements

        // Gateway
        Gateway gateway = new Gateway(GATEWAY_ID, new Position(482 * posScale, 360 * posScale));
        gateway.setView(allMotes);
        simul.addGateways(gateway);

        // Links
        int power = 15;
        int distribution = 100;
        mote2. addLinkTo(mote4,   gateway, power, distribution);
        mote3. addLinkTo(gateway, gateway, power, distribution);
        mote4. addLinkTo(gateway, gateway, power, distribution);
        mote5. addLinkTo(mote9,   gateway, power, distribution);
        mote6. addLinkTo(mote4,   gateway, power, distribution);
        mote7. addLinkTo(mote2,   gateway, power, distribution);
        mote7. addLinkTo(mote3,   gateway, power, distribution);
        mote8. addLinkTo(gateway, gateway, power, distribution);
        mote9. addLinkTo(gateway, gateway, power, distribution);
        mote10.addLinkTo(mote6,   gateway, power, distribution);
        mote10.addLinkTo(mote5,   gateway, power, distribution);
        mote11.addLinkTo(mote7,   gateway, power, distribution);
        mote12.addLinkTo(mote7,   gateway, power, distribution);
        mote12.addLinkTo(mote3,   gateway, power, distribution);
        mote13.addLinkTo(mote11,  gateway, power, distribution);
        mote14.addLinkTo(mote12,  gateway, power, distribution);
        mote15.addLinkTo(mote12,  gateway, power, distribution);

        // Set order
        simul.setTurnOrder(8, 10, 13, 14, 15, 5, 6, 11, 12, 9, 7, 2, 3, 4, 8);

        // Set profiles for some links and motes
        mote2 .setActivationProbability(new Constant<>(0.85));
        mote8 .setActivationProbability(new Constant<>(0.85));
        mote10.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR1.txt", 1.0));
        mote13.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR2.txt", 1.0));
        mote14.setActivationProbability(new Constant<>(0.85));

        mote12.getLinkTo(mote3).setInterference(new FileProfile("deltaiot/scenario_data/SNR2.txt", 0.0));

        // Add SNR equations (from Usman's settings class)
        mote2 .getLinkTo(mote4  ).setSnrEquation(new SNREquation( 0.0473684210526,		-5.29473684211));
        mote3 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.0280701754386,		 4.25263157895));
        mote4 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.119298245614,		-1.49473684211));
        mote5 .getLinkTo(mote9  ).setSnrEquation(new SNREquation(-0.019298245614,		 4.8));
        mote6 .getLinkTo(mote4  ).setSnrEquation(new SNREquation( 0.0175438596491,		-3.84210526316));
        mote7 .getLinkTo(mote3  ).setSnrEquation(new SNREquation( 0.168421052632,		 2.30526315789));
        mote7 .getLinkTo(mote2  ).setSnrEquation(new SNREquation(-0.0157894736842,		 3.77894736842));
        mote8 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.00350877192982,		 0.45263157895));
        mote9 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.0701754385965,		 2.89473684211));
        mote10.getLinkTo(mote6  ).setSnrEquation(new SNREquation( 3.51139336547e-16,		-2.21052631579));
        mote10.getLinkTo(mote5  ).setSnrEquation(new SNREquation( 0.250877192982,		-1.75789473684));
        mote11.getLinkTo(mote7  ).setSnrEquation(new SNREquation( 0.380701754386,		-2.12631578947));
        mote12.getLinkTo(mote7  ).setSnrEquation(new SNREquation( 0.317543859649,		 2.95789473684));
        mote12.getLinkTo(mote3  ).setSnrEquation(new SNREquation(-0.0157894736842,		-3.77894736842));
        mote13.getLinkTo(mote11 ).setSnrEquation(new SNREquation(-0.0210526315789,		-2.81052631579));
        mote14.getLinkTo(mote12 ).setSnrEquation(new SNREquation( 0.0333333333333,		 2.58947368421));
        mote15.getLinkTo(mote12 ).setSnrEquation(new SNREquation( 0.0438596491228,		 1.31578947368));

        // Global random interference (mimicking Usman's random interference) simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));
        simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));

        return simul;

    }

    public static Simulator createSimulatorCase1(int x, int y, double battery16, int load16, int neighId, double delta){
        /*
        case 1 = adding motes to the edge of the network with a lot of battery
         */
        Simulator simul = new Simulator();
        //System.out.println("post simulator");
        // Motes
        int load = 10; //number of packets to send in a turn; default ws 10
        double battery = 11880.0-delta; //default was 11880.0
        double posScale = 2; //default was 2
        Mote mote2  = new Mote(2 , battery, load, new Position(378 * posScale, 193 * posScale));
        Mote mote3  = new Mote(3 , battery, load, new Position(365 * posScale, 343 * posScale));
        Mote mote4  = new Mote(4 , battery, load, new Position(508 * posScale, 296 * posScale));
        Mote mote5  = new Mote(5 , battery, load, new Position(603 * posScale, 440 * posScale));
        Mote mote6  = new Mote(6 , battery, load, new Position(628 * posScale, 309 * posScale));
        Mote mote7  = new Mote(7 , battery, load, new Position(324 * posScale, 273 * posScale));
        Mote mote8  = new Mote(8 , battery, load, new Position(392 * posScale, 478 * posScale));
        Mote mote9  = new Mote(9 , battery, load, new Position(540 * posScale, 479 * posScale));
        Mote mote10 = new Mote(10, battery, load, new Position(694 * posScale, 356 * posScale));
        Mote mote11 = new Mote(11, battery, load, new Position(234 * posScale, 232 * posScale));
        Mote mote12 = new Mote(12, battery, load, new Position(221 * posScale, 322 * posScale));
        Mote mote13 = new Mote(13, battery, load, new Position(142 * posScale, 170 * posScale));
        Mote mote14 = new Mote(14, battery, load, new Position(139 * posScale, 293 * posScale));
        Mote mote15 = new Mote(15, battery, load, new Position(128 * posScale, 344 * posScale));
        Mote mote16  = new Mote(16 , battery16, load16, new Position(x * posScale, y * posScale)); //link it to 12


        Mote[] allMotes = new Mote[]{mote2, mote3, mote4, mote5, mote6, mote7, mote8, mote9, mote10, mote11, mote12, mote13, mote14, mote15, mote16};
        simul.addMotes(allMotes); //ignore the first null elements

        // Gateway
        Gateway gateway = new Gateway(GATEWAY_ID, new Position(482 * posScale, 360 * posScale));
        gateway.setView(allMotes);
        simul.addGateways(gateway);

        // Links
        int power = 15;
        int distribution = 100;
        mote2. addLinkTo(mote4,   gateway, power, distribution);
        mote3. addLinkTo(gateway, gateway, power, distribution);
        mote4. addLinkTo(gateway, gateway, power, distribution);
        mote5. addLinkTo(mote9,   gateway, power, distribution);
        mote6. addLinkTo(mote4,   gateway, power, distribution);
        mote7. addLinkTo(mote2,   gateway, power, distribution);
        mote7. addLinkTo(mote3,   gateway, power, distribution);
        mote8. addLinkTo(gateway, gateway, power, distribution);
        mote9. addLinkTo(gateway, gateway, power, distribution);
        mote10.addLinkTo(mote6,   gateway, power, distribution);
        mote10.addLinkTo(mote5,   gateway, power, distribution);
        mote11.addLinkTo(mote7,   gateway, power, distribution);
        mote12.addLinkTo(mote7,   gateway, power, distribution);
        mote12.addLinkTo(mote3,   gateway, power, distribution);
        mote13.addLinkTo(mote11,  gateway, power, distribution);
        mote14.addLinkTo(mote12,  gateway, power, distribution);
        mote15.addLinkTo(mote12,  gateway, power, distribution);
        //16 connects to its very neighbour, and i only know its id

        mote16.addLinkTo(allMotes[neighId-2],  gateway, power, distribution);

        // Set order
        simul.setTurnOrder(8, 10, 13, 16, 14, 15, 5, 16, 6, 11, 12, 16, 9, 7, 2, 3, 16, 4, 8);

        // Set profiles for some links and motes
        mote2 .setActivationProbability(new Constant<>(0.85));
        mote8 .setActivationProbability(new Constant<>(0.85));
        mote10.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR1.txt", 1.0));
        mote13.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR2.txt", 1.0));
        mote14.setActivationProbability(new Constant<>(0.85));

        mote12.getLinkTo(mote3).setInterference(new FileProfile("deltaiot/scenario_data/SNR2.txt", 0.0));

        // Add SNR equations (from Usman's settings class)
        mote2 .getLinkTo(mote4  ).setSnrEquation(new SNREquation( 0.0473684210526,		-5.29473684211));
        mote3 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.0280701754386,		 4.25263157895));
        mote4 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.119298245614,		-1.49473684211));
        mote5 .getLinkTo(mote9  ).setSnrEquation(new SNREquation(-0.019298245614,		 4.8));
        mote6 .getLinkTo(mote4  ).setSnrEquation(new SNREquation( 0.0175438596491,		-3.84210526316));
        mote7 .getLinkTo(mote3  ).setSnrEquation(new SNREquation( 0.168421052632,		 2.30526315789));
        mote7 .getLinkTo(mote2  ).setSnrEquation(new SNREquation(-0.0157894736842,		 3.77894736842));
        mote8 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.00350877192982,		 0.45263157895));
        mote9 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.0701754385965,		 2.89473684211));
        mote10.getLinkTo(mote6  ).setSnrEquation(new SNREquation( 3.51139336547e-16,		-2.21052631579));
        mote10.getLinkTo(mote5  ).setSnrEquation(new SNREquation( 0.250877192982,		-1.75789473684));
        mote11.getLinkTo(mote7  ).setSnrEquation(new SNREquation( 0.380701754386,		-2.12631578947));
        mote12.getLinkTo(mote7  ).setSnrEquation(new SNREquation( 0.317543859649,		 2.95789473684));
        mote12.getLinkTo(mote3  ).setSnrEquation(new SNREquation(-0.0157894736842,		-3.77894736842));
        mote13.getLinkTo(mote11 ).setSnrEquation(new SNREquation(-0.0210526315789,		-2.81052631579));
        mote14.getLinkTo(mote12 ).setSnrEquation(new SNREquation( 0.0333333333333,		 2.58947368421));
        mote15.getLinkTo(mote12 ).setSnrEquation(new SNREquation( 0.0438596491228,		 1.31578947368));
        mote16.getLinkTo(allMotes[neighId-2] ).setSnrEquation(new SNREquation( 0.0438596491228,		 1.31578947368)); //TODO what is up w this values

        // Global random interference (mimicking Usman's random interference) simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));
        simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));

        return simul;




    }

    public static Simulator createSimulatorUnknown(int[] x, int[] y, double battery16, int load16, int[] neighId, double delta){
        /*
        unknown = adding 2 motes to the edge of the network with a lot of battery
         */

        Simulator simul = new Simulator();
        //System.out.println("post simulator");
        // Motes
        int load = 10; //number of packets to send in a turn; default ws 10
        double battery = 11880.0-delta; //default was 11880.0
        double posScale = 2; //default was 2
        Mote mote2  = new Mote(2 , battery, load, new Position(378 * posScale, 193 * posScale));
        Mote mote3  = new Mote(3 , battery, load, new Position(365 * posScale, 343 * posScale));
        Mote mote4  = new Mote(4 , battery, load, new Position(508 * posScale, 296 * posScale));
        Mote mote5  = new Mote(5 , battery, load, new Position(603 * posScale, 440 * posScale));
        Mote mote6  = new Mote(6 , battery, load, new Position(628 * posScale, 309 * posScale));
        Mote mote7  = new Mote(7 , battery, load, new Position(324 * posScale, 273 * posScale));
        Mote mote8  = new Mote(8 , battery, load, new Position(392 * posScale, 478 * posScale));
        Mote mote9  = new Mote(9 , battery, load, new Position(540 * posScale, 479 * posScale));
        Mote mote10 = new Mote(10, battery, load, new Position(694 * posScale, 356 * posScale));
        Mote mote11 = new Mote(11, battery, load, new Position(234 * posScale, 232 * posScale));
        Mote mote12 = new Mote(12, battery, load, new Position(221 * posScale, 322 * posScale));
        Mote mote13 = new Mote(13, battery, load, new Position(142 * posScale, 170 * posScale));
        Mote mote14 = new Mote(14, battery, load, new Position(139 * posScale, 293 * posScale));
        Mote mote15 = new Mote(15, battery, load, new Position(128 * posScale, 344 * posScale));
        Mote mote16  = new Mote(16 , battery16, load16, new Position(x[0] * posScale, y[0] * posScale)); //link it to 12
        Mote mote17  = new Mote(17 , battery16, load16, new Position(x[1] * posScale, y[1] * posScale)); //link it to 12


        Mote[] allMotes = new Mote[]{mote2, mote3, mote4, mote5, mote6, mote7, mote8, mote9, mote10, mote11, mote12, mote13, mote14, mote15, mote16, mote17};
        simul.addMotes(allMotes); //ignore the first null elements

        // Gateway
        Gateway gateway = new Gateway(GATEWAY_ID, new Position(482 * posScale, 360 * posScale));
        gateway.setView(allMotes);
        simul.addGateways(gateway);

        // Links
        int power = 15;
        int distribution = 100;
        mote2. addLinkTo(mote4,   gateway, power, distribution);
        mote3. addLinkTo(gateway, gateway, power, distribution);
        mote4. addLinkTo(gateway, gateway, power, distribution);
        mote5. addLinkTo(mote9,   gateway, power, distribution);
        mote6. addLinkTo(mote4,   gateway, power, distribution);
        mote7. addLinkTo(mote2,   gateway, power, distribution);
        mote7. addLinkTo(mote3,   gateway, power, distribution);
        mote8. addLinkTo(gateway, gateway, power, distribution);
        mote9. addLinkTo(gateway, gateway, power, distribution);
        mote10.addLinkTo(mote6,   gateway, power, distribution);
        mote10.addLinkTo(mote5,   gateway, power, distribution);
        mote11.addLinkTo(mote7,   gateway, power, distribution);
        mote12.addLinkTo(mote7,   gateway, power, distribution);
        mote12.addLinkTo(mote3,   gateway, power, distribution);
        mote13.addLinkTo(mote11,  gateway, power, distribution);
        mote14.addLinkTo(mote12,  gateway, power, distribution);
        mote15.addLinkTo(mote12,  gateway, power, distribution);
        //16 (and 17) connects to its very neighbour, and i only know its id

        mote16.addLinkTo(allMotes[neighId[0]-2],  gateway, power, distribution);

        mote17.addLinkTo(allMotes[neighId[1]-2],  gateway, power, distribution);

        // Set order
        simul.setTurnOrder(8, 10, 13, 16, 17, 14, 15, 5, 16, 6, 11, 12, 16, 17, 9, 7, 2, 3, 16, 4, 8);

        // Set profiles for some links and motes
        mote2 .setActivationProbability(new Constant<>(0.85));
        mote8 .setActivationProbability(new Constant<>(0.85));
        mote10.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR1.txt", 1.0));
        mote13.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR2.txt", 1.0));
        mote14.setActivationProbability(new Constant<>(0.85));

        mote12.getLinkTo(mote3).setInterference(new FileProfile("deltaiot/scenario_data/SNR2.txt", 0.0));

        // Add SNR equations (from Usman's settings class)
        mote2 .getLinkTo(mote4  ).setSnrEquation(new SNREquation( 0.0473684210526,		-5.29473684211));
        mote3 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.0280701754386,		 4.25263157895));
        mote4 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.119298245614,		-1.49473684211));
        mote5 .getLinkTo(mote9  ).setSnrEquation(new SNREquation(-0.019298245614,		 4.8));
        mote6 .getLinkTo(mote4  ).setSnrEquation(new SNREquation( 0.0175438596491,		-3.84210526316));
        mote7 .getLinkTo(mote3  ).setSnrEquation(new SNREquation( 0.168421052632,		 2.30526315789));
        mote7 .getLinkTo(mote2  ).setSnrEquation(new SNREquation(-0.0157894736842,		 3.77894736842));
        mote8 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.00350877192982,		 0.45263157895));
        mote9 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.0701754385965,		 2.89473684211));
        mote10.getLinkTo(mote6  ).setSnrEquation(new SNREquation( 3.51139336547e-16,		-2.21052631579));
        mote10.getLinkTo(mote5  ).setSnrEquation(new SNREquation( 0.250877192982,		-1.75789473684));
        mote11.getLinkTo(mote7  ).setSnrEquation(new SNREquation( 0.380701754386,		-2.12631578947));
        mote12.getLinkTo(mote7  ).setSnrEquation(new SNREquation( 0.317543859649,		 2.95789473684));
        mote12.getLinkTo(mote3  ).setSnrEquation(new SNREquation(-0.0157894736842,		-3.77894736842));
        mote13.getLinkTo(mote11 ).setSnrEquation(new SNREquation(-0.0210526315789,		-2.81052631579));
        mote14.getLinkTo(mote12 ).setSnrEquation(new SNREquation( 0.0333333333333,		 2.58947368421));
        mote15.getLinkTo(mote12 ).setSnrEquation(new SNREquation( 0.0438596491228,		 1.31578947368));
        mote16.getLinkTo(allMotes[neighId[0]-2] ).setSnrEquation(new SNREquation( 0.0438596491228,		 1.31578947368));
        mote17.getLinkTo(allMotes[neighId[1]-2] ).setSnrEquation(new SNREquation( 0.0438596491228,		 1.31578947368));

        // Global random interference (mimicking Usman's random interference) simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));
        simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));

        return simul;




    }

}
