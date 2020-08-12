package com.amhsrobotics.circuitsim.hardware.devices;

import com.amhsrobotics.circuitsim.files.JSONReader;
import com.amhsrobotics.circuitsim.gui.CircuitGUIManager;
import com.amhsrobotics.circuitsim.hardware.Hardware;
import com.amhsrobotics.circuitsim.hardware.HardwareType;
import com.amhsrobotics.circuitsim.wiring.Cable;
import com.amhsrobotics.circuitsim.wiring.CrimpedCable;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import me.rohanbansal.ricochet.tools.ModifiedShapeRenderer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SandCrab extends Hardware {


    public SandCrab(Vector2 position, HardwareType type, boolean... addCrimped) {
        super(position, addCrimped);

        this.type = type;

        if(type == HardwareType.DOUBLESANDCRAB) {
            JSONReader.loadConfig("scripts/DoubleSandCrab.json");
            base = new Sprite(new Texture(Gdx.files.internal("img/hardware/wago2.png")));
        } else {
            JSONReader.loadConfig("scripts/TripleSandCrab.json");
            base = new Sprite(new Texture(Gdx.files.internal("img/hardware/wago3.png")));
        }

        connNum = ((Long) JSONReader.getCurrentConfig().get("totalPins")).intValue();
        name = (String) (JSONReader.getCurrentConfig().get("name"));
        JSONArray pins = (JSONArray) JSONReader.getCurrentConfig().get("pins");
        for(int x = 0; x < pins.size(); x++) {
            pinDefs.add((JSONArray) ((JSONObject) pins.get(x)).get("position"));
            pinSizeDefs.add((JSONArray) ((JSONObject) pins.get(x)).get("dimensions"));
            portTypes.add((String) ((JSONObject) pins.get(x)).get("type"));
        }

        base.setCenter(position.x, position.y);

        for(JSONArray arr : pinDefs) {
            Sprite temp;
            if(connectors.size() == connNum) {
                break;
            }
//            if(connectors.size() == 0) {
//                temp = new Sprite(new Texture(Gdx.files.internal("img/hardware/sandcrab_orange_2.png")));
//            } else {
//                temp = new Sprite(new Texture(Gdx.files.internal("img/hardware/sandcrab_orange.png")));
//            }
            temp = new Sprite(new Texture(Gdx.files.internal("img/hardware/wago_orange.png")));

            temp.setCenter(position.x + (Long) arr.get(0), position.y + (Long) arr.get(1));
//            temp.setSize((Long)pinSizeDefs.get(pinDefs.indexOf(arr)).get(0), (Long)pinSizeDefs.get(pinDefs.indexOf(arr)).get(1));

            connectors.add(temp);
        }

        initConnections();
        initEnds();
    }


    public Vector2 calculate(int port) {
        return new Vector2(getConnector(port).getX() + getConnector(port).getWidth() / 2, getConnector(port).getY() + getConnector(port).getHeight()/2 - 20);
    }



    @Override
    public void populateProperties() {
        super.populateProperties();
        for(int x = 0; x < connectors.size(); x++) {
            CircuitGUIManager.propertiesBox.addElement(new Label("Conn. " + (x + 1), CircuitGUIManager.propertiesBox.LABEL_SMALL), true, 1);
            CircuitGUIManager.propertiesBox.addElement(new Label(connections.get(x) == null ? "None" : (connections.get(x) instanceof CrimpedCable ? "Crimped" : "Cable " + connections.get(x).getID()), CircuitGUIManager.propertiesBox.LABEL_SMALL), false, 1);
        }
    }

    @Override
    public void attachWireLib(Cable cable, int port, boolean endOfWire) {
        cable.addCoordinates(new Vector2(getConnector(port).getX() + getConnector(port).getWidth() / 2, getConnector(port).getY() - 20), !endOfWire);
        cable.addCoordinates(new Vector2(getConnector(port).getX() + getConnector(port).getWidth() / 2, getConnector(port).getY() + 20), !endOfWire);

        if(endOfWire) {cable.setConnection2(this);} else {cable.setConnection1(this);}
    }

    public void editWire(Cable cable, int port, boolean endOfWire) {
        cable.editCoordinates(new Vector2(getConnector(port).getX() + getConnector(port).getWidth() / 2, getConnector(port).getY() + 20), endOfWire, false);
        cable.editCoordinates(new Vector2(getConnector(port).getX() + getConnector(port).getWidth() / 2, getConnector(port).getY() -20), endOfWire, true);
    }

    public void drawHover(ModifiedShapeRenderer renderer) {
        renderer.setColor(new Color(156/255f,1f,150/255f,1f));

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.roundedRect(getPosition().x - (base.getWidth() / 2)-7, getPosition().y - (base.getHeight() / 2)-5, base.getWidth()+12, base.getHeight()+11, 15);
        renderer.end();
    }
}
