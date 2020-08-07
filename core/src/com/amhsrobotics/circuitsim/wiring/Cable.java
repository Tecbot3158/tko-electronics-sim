package com.amhsrobotics.circuitsim.wiring;

import com.amhsrobotics.circuitsim.gui.CircuitGUIManager;
import com.amhsrobotics.circuitsim.hardware.DoubleSandCrab;
import com.amhsrobotics.circuitsim.hardware.Hardware;
import com.amhsrobotics.circuitsim.hardware.HardwareManager;
import com.amhsrobotics.circuitsim.hardware.TripleSandCrab;
import com.amhsrobotics.circuitsim.utility.ClippedCameraController;
import com.amhsrobotics.circuitsim.utility.DeviceUtil;
import com.amhsrobotics.circuitsim.utility.SnapGrid;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import me.rohanbansal.ricochet.camera.CameraController;
import me.rohanbansal.ricochet.tools.ModifiedShapeRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Cable implements Disposable {

    private float voltage;
    private float gauge;
    private Color color;
    private Color hoverColor = Color.WHITE;
    private ArrayList<Vector2> coordinates;
    private Hardware connection1, connection2;
    private float x1, x2, y1, y2, a;

    private boolean appendingFromEnd, appendingFromBegin;
    private boolean nodeChanged = false;
    private Vector2 movingNode, backupNode;

    private boolean disableEnd, disableBegin;

    private float limit;


    public Cable(float voltage, float gauge, ArrayList<Vector2> coordinates) {
        this.voltage = voltage;
        this.gauge = gauge;
        this.coordinates = coordinates;
        this.color = DeviceUtil.COLORS.get("Green");

        populateProperties();
    }

    public Cable(Vector2 startPoint) {
        voltage = 0;
        gauge = DeviceUtil.GAUGES[0];
        coordinates = new ArrayList<>();
        this.color = DeviceUtil.COLORS.get("Green");

        coordinates.add(startPoint);

        populateProperties();
    }

    private void populateProperties() {
        CircuitGUIManager.propertiesBox.clearTable();
        if(CircuitGUIManager.propertiesBox.isVisible()) {
            CircuitGUIManager.propertiesBox.addElement(new Label("Cable", CircuitGUIManager.propertiesBox.LABEL), true, 2);
            CircuitGUIManager.propertiesBox.addElement(new Label("Color", CircuitGUIManager.propertiesBox.LABEL_SMALL), true, 1);
            final TextButton cb = new TextButton(DeviceUtil.getKeyByValue(DeviceUtil.COLORS, this.color), CircuitGUIManager.propertiesBox.TBUTTON);
            CircuitGUIManager.propertiesBox.addElement(cb, false, 1);

            CircuitGUIManager.propertiesBox.addElement(new Label("Gauge", CircuitGUIManager.propertiesBox.LABEL_SMALL), true, 1);
            final TextButton ga = new TextButton(this.gauge + "", CircuitGUIManager.propertiesBox.TBUTTON);
            CircuitGUIManager.propertiesBox.addElement(ga, false, 1);

            cb.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    ArrayList<String> keys = new ArrayList<>(DeviceUtil.COLORS.keySet());
                    for(String str : keys) {
                        if(str.contentEquals(cb.getText())) {
                            if(keys.indexOf(str) == keys.size() - 1) {
                                cb.setText(keys.get(0));
                                color = DeviceUtil.COLORS.get(keys.get(0));
                            } else {
                                cb.setText(keys.get(keys.indexOf(str) + 1));
                                color = DeviceUtil.COLORS.get(keys.get(keys.indexOf(str) + 1));
                            }
                            break;
                        }
                    }
                }
            });
            ga.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    List<Integer> gauges = Arrays.stream(DeviceUtil.GAUGES).boxed().collect(Collectors.toList());
                    for(int gau : gauges) {
                        if(gau == gauge) {
                            if(gauges.indexOf(gau) == gauges.size() - 1) {
                                gauge = gauges.get(0);
                                ga.setText(gauge + "");
                            } else {
                                gauge = gauges.get(gauges.indexOf(gau) + 1);
                                ga.setText(gauge + "");
                            }
                            break;
                        }
                    }
                }
            });
        }
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void addCoordinates(Vector2 point, boolean begin) {
        if(begin) {
            this.coordinates.add(0, point);
        } else {
            this.coordinates.add(point);
        }
    }

    public void editCoordinates(Vector2 point, boolean endOfWire) {
        if(endOfWire) {
            this.coordinates.set(this.coordinates.size()-1, point);
        } else {
            this.coordinates.set(0, point);
        }

    }

    public void update(ModifiedShapeRenderer renderer, ClippedCameraController camera) {

        limit = (gauge/2f + 3f)/2;

        renderer.setProjectionMatrix(camera.getCamera().combined);
        renderer.begin(ShapeRenderer.ShapeType.Filled);

        // LOGIC
        // ---------------------------------------------------------------------
        if(nodeChanged) {
            nodeChanged = false;
        }
        disableBegin = connection1 != null;
        disableEnd = connection2 != null;
        // ---------------------------------------------------------------------

        // DRAW CABLE
        // ---------------------------------------------------------------------
        renderer.setColor(color);
        for(int i = 0; i < coordinates.size() - 1; ++i) {
            if(CableManager.currentCable != null) {
                if(CableManager.currentCable == this) {
                    // draw cable selected
                    renderer.setColor(new Color(217/255f, 233/255f, 217/255f, 1));
                    renderer.rectLine(coordinates.get(i), coordinates.get(i + 1), gauge/2f + 3f);
                }
            }
            if(hoveringMouse(camera)) {
                // draw hovering on cable
                renderer.setColor(new Color(217 / 255f, 233 / 255f, 217 / 255f, 1));
                renderer.rectLine(coordinates.get(i), coordinates.get(i + 1), gauge/2f + 3f);
            }
            // draw actual cable
            renderer.setColor(color);
            renderer.rectLine(coordinates.get(i), coordinates.get(i + 1), gauge/2f);

            renderer.circle(coordinates.get(i).x, coordinates.get(i).y, limit);
        }
        // ---------------------------------------------------------------------

        // CABLE SELECTED MECHANICS
        // ---------------------------------------------------------------------
        if(CableManager.currentCable == this) {
            Vector3 vec = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.getCamera().unproject(vec);
            Vector2 vec2 = new Vector2(vec.x, vec.y);

            if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                SnapGrid.calculateSnap(vec2);
            }

            if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                CableManager.currentCable = null;
                appendingFromBegin = false;
                appendingFromEnd = false;
                if(movingNode != null) {
                    coordinates.set(coordinates.indexOf(movingNode), backupNode);
                    movingNode = null;
                    backupNode = null;
                }
                CircuitGUIManager.propertiesBox.hideAndClear();
            }

            if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                HashMap<Hardware, Integer> hardware = HardwareManager.wireHoveringHardware(vec2);
                if(hardware != null) {
                    processHardwareClick(hardware);
                } else {
                    if(appendingFromEnd && !disableEnd) {
                        addCoordinates(new Vector2(vec2.x, vec2.y), false);
                    } else if(appendingFromBegin && !disableBegin) {
                        addCoordinates(new Vector2(vec2.x, vec2.y), true);
                    } else if(movingNode != null && backupNode.x != movingNode.x && backupNode.y != movingNode.y) {
                        coordinates.set(coordinates.indexOf(movingNode), new Vector2(vec2.x, vec2.y));
                        movingNode = null;
                        backupNode = null;
                        nodeChanged = true;
                    }
                }

            }

            if((Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL) || Gdx.input.isKeyJustPressed(Input.Keys.DEL)) && movingNode == null) {
                CableManager.deleteCable(this);
                CableManager.currentCable = null;
                CircuitGUIManager.propertiesBox.hide();
                HardwareManager.removeCableFromHardware(this, connection1);
                HardwareManager.removeCableFromHardware(this, connection2);
                connection2 = null;
                connection1 = null;
            }

            drawNodes(renderer, camera, Color.SALMON);

            if(appendingFromEnd && !disableEnd) {
                // draw potential cable wire
                renderer.setColor(color);
                renderer.rectLine(coordinates.get(coordinates.size() - 1), new Vector2(vec2.x, vec2.y), gauge/2f);
                renderer.setColor(Color.SALMON);
                renderer.circle(vec2.x, vec2.y, limit);
            } else if(appendingFromBegin && !disableBegin) {
                // draw potential cable wire
                renderer.setColor(color);
                renderer.rectLine(coordinates.get(0), new Vector2(vec2.x, vec2.y), gauge/2f);
                renderer.setColor(Color.SALMON);
                renderer.circle(vec2.x, vec2.y, limit);
            } else if(movingNode != null) {
                movingNode.set(vec2.x, vec2.y);
                if(Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL) || Gdx.input.isKeyJustPressed(Input.Keys.DEL)) {
                    coordinates.remove(movingNode);
                    movingNode = null;
                    backupNode = null;
                }
            }

        }
        // ---------------------------------------------------------------------

        // HOVERING OVER CABLE
        // ---------------------------------------------------------------------
        if(hoveringMouse(camera)) {
            drawNodes(renderer, camera, Color.SALMON);
            checkForClick(camera);
        }

        // ---------------------------------------------------------------------

        renderer.end();
    }

    private void processHardwareClick(HashMap<Hardware, Integer> hardware) {
        ArrayList<Hardware> clist = new ArrayList<>(hardware.keySet());
        if(clist.get(0) instanceof DoubleSandCrab) {
            DoubleSandCrab crab = (DoubleSandCrab) clist.get(0);
            if(appendingFromEnd) {
                crab.attachWire(this, hardware.get(clist.get(0)), true);
            } else if(appendingFromBegin) {
                crab.attachWire(this, hardware.get(clist.get(0)), false);
            }
        } else if(clist.get(0) instanceof TripleSandCrab) {
            TripleSandCrab crab = (TripleSandCrab) clist.get(0);
            if(appendingFromEnd) {
                crab.attachWire(this, hardware.get(clist.get(0)), true);
            } else if(appendingFromBegin) {
                crab.attachWire(this, hardware.get(clist.get(0)), false);
            }
        }
    }

    private void checkForClick(ClippedCameraController camera) {
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && (Gdx.input.getX() <= Gdx.graphics.getWidth() - 200)) {
            if(hoveringOnEndpoint(camera) == 1) {
                appendingFromBegin = true;
                appendingFromEnd = false;
                HardwareManager.currentHardware = null;
            } else if(hoveringOnEndpoint(camera) == 2) {
                appendingFromEnd = true;
                appendingFromBegin = false;
                HardwareManager.currentHardware = null;
            } else if(hoveringOnNode(camera) != null && movingNode == null && !nodeChanged) {
                movingNode = hoveringOnNode(camera);
                backupNode = new Vector2(hoveringOnNode(camera));
            }

            if(CableManager.currentCable != this) {
                CableManager.currentCable = this;
                CircuitGUIManager.propertiesBox.show();
                HardwareManager.currentHardware = null;
                populateProperties();
            }
        }
    }

    private Vector2 hoveringOnNode(ClippedCameraController camera) {
        Vector3 vec = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.getCamera().unproject(vec);

        for(Vector2 coord : coordinates) {
            if(coordinates.indexOf(coord) != 0 && coordinates.indexOf(coord) != coordinates.size() - 1) {
                if(new Circle(coord.x, coord.y, limit).contains(vec.x, vec.y)) {
                    return coord;
                }
            }
        }
        return null;
    }

    private void drawNodes(ShapeRenderer renderer, ClippedCameraController cam, Color... color) {

        if(color.length > 0) {
            renderer.setColor(color[0]);
        }
        for(Vector2 coords : coordinates) {
            renderer.circle(coords.x, coords.y, limit);
        }
        if(hoveringOnEndpoint(cam) == 1) {
            renderer.setColor(hoverColor);
            renderer.circle(coordinates.get(0).x, coordinates.get(0).y, limit+3f);
        } else if(hoveringOnEndpoint(cam) == 2) {
            renderer.setColor(hoverColor);
            renderer.circle(coordinates.get(coordinates.size() - 1).x, coordinates.get(coordinates.size() - 1).y, limit+3f);
        } else if(hoveringOnNode(cam) != null) {
            if(movingNode == null) {
                renderer.setColor(hoverColor);
                renderer.circle(hoveringOnNode(cam).x, hoveringOnNode(cam).y, limit);
            }
        }

    }

    public int hoveringOnEndpoint(CameraController cameraController) {
        Vector3 vec = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        cameraController.getCamera().unproject(vec);

        Vector2 c2 = coordinates.get(coordinates.size() - 1);
        Vector2 c = coordinates.get(0);

        if(new Circle(c2.x, c2.y, limit).contains(vec.x, vec.y)) {
            return 2;
        } else if(new Circle(c.x, c.y, limit).contains(vec.x, vec.y)) {
            return 1;
        }
        return 0;
    }

    public Hardware getConnection1() {
        return connection1;
    }

    public void setConnection1(Hardware connection1) {
        this.connection1 = connection1;
    }

    public Hardware getConnection2() {
        return connection2;
    }

    public void setConnection2(Hardware connection2) {
        this.connection2 = connection2;
    }

    public float getVoltage() {
        return voltage;
    }

    public float getGauge() {
        return gauge;
    }

    public ArrayList<Vector2> getCoordinates() {
        return coordinates;
    }

    public void setAppendingFromEnd(boolean appendingFromEnd) {
        this.appendingFromEnd = appendingFromEnd;
    }

    public void setAppendingFromBegin(boolean appendingFromBegin) {
        this.appendingFromBegin = appendingFromBegin;
    }

    public boolean hoveringMouse(CameraController cameraController) {
        Vector3 vec = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        cameraController.getCamera().unproject(vec);
        float x = vec.x;
        float y = vec.y;

        if(hoveringOnEndpoint(cameraController) != 0) {
            return true;
        }

        for(int i = 0; i < coordinates.size() - 1; ++i) {
            x1 = coordinates.get(i).x;
            x2 = coordinates.get(i + 1).x;
            y1 = coordinates.get(i).y;
            y2 = coordinates.get(i + 1).y;

            if(x1 == x2 && x > x1-limit && x < x1+limit && ((y1 < y2 && y >= y1 && y <= y2)||(y1 > y2 && y <= y1 && y >= y2))) {
                return true;
            }

            if(y1 == y2 && y > y1-limit && y < y1+limit && ((x1 < x2 && x >= x1 && x <= x2)||(x1 > x2 && x <= x1 && x >= x2))) {
                return true;
            }

            if(((x1 < x2 && x >= x1 && x <= x2)||(x1 > x2 && x <= x1 && x >= x2))&&((y1 < y2 && y >= y1 && y <= y2)||(y1 > y2 && y <= y1 && y >= y2))) {

                a = -1 * ((y2 - y1) / (x2 - x1));

                if ((float) Math.abs(x * a + y + (((y2 - y1) / (x2 - x1)) * x1 - y1)) / Math.sqrt(a * a + 1) < limit) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void dispose() {
    }
}
