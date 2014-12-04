package org.openstreetmap.gui.jmapviewer;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.openstreetmap.gui.app.SettingManager;
import org.openstreetmap.gui.persistence.XMLWriter;

/**
 * Default map controller which implements map moving by pressing the right
 * mouse button and zooming by double click or by mouse wheel.
 *
 * @author Jan Peter Stotz
 * @author Martin P. Robillard Marker display
 *
 */
public class JMapController implements MouseListener, MouseMotionListener, MouseWheelListener 
{
    private static final int MOUSE_BUTTONS_MASK = MouseEvent.BUTTON3_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
    private static final int MAC_MOUSE_BUTTON3_MASK = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
    
    private static final boolean MOVEMENT_ENABLED = true;
    private static final boolean WHEEL_ZOOM_ENABLED = true;
    private static final boolean DOUBLE_CLICK_ZOOM_ENABLED = true;
    
    private JMapViewer aMap;
    private Point aLastDragPoint;
    private Point aLastClickedPoint;
    private boolean aIsMoving = false;
    private int aMovementMouseButton = MouseEvent.BUTTON3;
    private int aMovementMouseButtonMask = MouseEvent.BUTTON3_DOWN_MASK;
    
    /**
     * Creates a new controller for the JMapViewer.
     * @param pMap The object that is controlled.
     */
    public JMapController(JMapViewer pMap) 
    {
        aMap = pMap;
        setMovementMouseButton(MouseEvent.BUTTON1); 
        aMap.addMouseListener((MouseListener) this);
        aMap.addMouseWheelListener((MouseWheelListener) this);
        aMap.addMouseMotionListener((MouseMotionListener) this);
    }
    
    private void maybeShowPopup(MouseEvent pEvent) 
    {
        if( pEvent.isPopupTrigger() )
        {
            aLastClickedPoint = pEvent.getPoint();
            MapMarker[] lMarkers = aMap.getMapMarkersAt(aLastClickedPoint);
            if( lMarkers.length > 1 )
            {
                JOptionPane.showMessageDialog(aMap, "Multiple markers selected. Select either a single marker, or an unmarked area of the map.", 
                		"Marker Selection Error", JOptionPane.ERROR_MESSAGE);
            }
            else if( lMarkers.length == 0 )
            {
                JPopupMenu lPopup = new JPopupMenu();
                JMenuItem menuItem = new JMenuItem("Add Marker");
                menuItem.addActionListener(new ActionListener() 
                {
                    public void actionPerformed(ActionEvent pEvent) 
                    {
                        MarkerInputPanel lMIP = new MarkerInputPanel();
                        int lResult = JOptionPane.showConfirmDialog(aMap, lMIP, "New Marker Data", JOptionPane.OK_CANCEL_OPTION);
                        Coordinate lCoord = aMap.getPosition((int)aLastClickedPoint.getX(), (int)aLastClickedPoint.getY());
                        if( lResult == JOptionPane.OK_OPTION )
                        {    
                            aMap.addMapMarker(new MapMarkerDot(lCoord.getLatitude(), lCoord.getLongitude(), 
                            		lMIP.getMarkerName(), lMIP.getDescription()));
                        
                            List<MapMarker> lMarkers = aMap.getMapMarkerList();
                            try
                            {
                                String lFile = SettingManager.getInstance().getDataFileName();
                                XMLWriter.backup(lFile);
                                XMLWriter.write((MapMarker[])lMarkers.toArray(new MapMarker[lMarkers.size()]), lFile);
                            }
                            catch( Exception exception)
                            {}
                        }
                    }
                });
                lPopup.add(menuItem); 
                lPopup.show(pEvent.getComponent(), pEvent.getX(), pEvent.getY());
            }
            else if( lMarkers.length == 1 )
            {
                // Delete and edit are supported
                JPopupMenu lPopup = new JPopupMenu();
                JMenuItem menuItem = new JMenuItem("Delete Marker");
                final MapMarker lSelected = lMarkers[0];
                menuItem.addActionListener( new ActionListener() 
                {
                    public void actionPerformed(ActionEvent pEvent)
                    {
                        int lResult = JOptionPane.showConfirmDialog(aMap, "Delete maker \"" + lSelected.getName() + 
                        		"\"\nAre you sure?" , "Confirm Marker Deletion", JOptionPane.YES_NO_OPTION);
                        if( lResult == JOptionPane.YES_OPTION )
                        {
                            aMap.removeMapMarker(lSelected);
                            List<MapMarker> lMarkers = aMap.getMapMarkerList();
                            try
                            {
                                String lFile = SettingManager.getInstance().getDataFileName();
                                XMLWriter.backup(lFile);
                                XMLWriter.write((MapMarker[])lMarkers.toArray(new MapMarker[lMarkers.size()]), lFile);
                            }
                            catch( Exception exception)
                            {}
                        }
                    }
                });
                lPopup.add(menuItem);
                
                menuItem = new JMenuItem("Edit Marker");
                menuItem.addActionListener( new ActionListener() 
                {
                    public void actionPerformed(ActionEvent pEvent) 
                    {
                        MarkerInputPanel lMIP = new MarkerInputPanel(lSelected.getName(), lSelected.getDescription());
                        int lResult = JOptionPane.showConfirmDialog(aMap, lMIP, "Edit Marker Data", JOptionPane.OK_CANCEL_OPTION);
                        if( lResult == JOptionPane.OK_OPTION )
                        {    
                            lSelected.setName(lMIP.getMarkerName());
                            lSelected.setDescription(lMIP.getDescription());
                            List<MapMarker> lMarkers = aMap.getMapMarkerList();
                            try
                            {
                                String lFile = SettingManager.getInstance().getDataFileName();
                                XMLWriter.backup(lFile);
                                XMLWriter.write((MapMarker[])lMarkers.toArray(new MapMarker[lMarkers.size()]), lFile);
                            }
                            catch( Exception exception)
                            {}
                        }
                    }
                });
                
                lPopup.add(menuItem); 
                lPopup.show(pEvent.getComponent(), pEvent.getX(), pEvent.getY());
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent pEvent) 
    {
        if (!MOVEMENT_ENABLED || !aIsMoving)
        {
            return;
        }
        // Is only the selected mouse button pressed?
        if((pEvent.getModifiersEx() & MOUSE_BUTTONS_MASK) == aMovementMouseButtonMask) 
        {
            Point p = pEvent.getPoint();
            if (aLastDragPoint != null) 
            {
                int diffx = aLastDragPoint.x - p.x;
                int diffy = aLastDragPoint.y - p.y;
                aMap.moveMap(diffx, diffy);
            }
            aLastDragPoint = p;
        }
    }

    @Override
    public void mouseClicked(MouseEvent pEvent) 
    {
        if( DOUBLE_CLICK_ZOOM_ENABLED && pEvent.getClickCount() == 2 && pEvent.getButton() == MouseEvent.BUTTON1 ) 
        {
            aMap.zoomIn(pEvent.getPoint());
        }
        else if( pEvent.getClickCount() == 1 && pEvent.getButton() == MouseEvent.BUTTON2 )
        {
            MapMarker[] lMarkers = aMap.getMapMarkersAt(pEvent.getPoint());
            String lMessage = "";
            for( MapMarker marker : lMarkers )
            {
                lMessage += marker.getName() + "\n" + marker.getDescription() + "\n";
            }
            JOptionPane.showMessageDialog(aMap, lMessage, "Location Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void mousePressed(MouseEvent pEvent) 
    {
        maybeShowPopup(pEvent);
        if (pEvent.getButton() == aMovementMouseButton || isPlatformOsx() && pEvent.getModifiersEx() == MAC_MOUSE_BUTTON3_MASK)
        {
            aLastDragPoint = null;
            aIsMoving = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent pEvent) 
    {
        maybeShowPopup(pEvent);
        if (pEvent.getButton() == aMovementMouseButton || isPlatformOsx() && pEvent.getButton() == MouseEvent.BUTTON1)
        {
            aLastDragPoint = null;
            aIsMoving = false;
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent pEvent) 
    {
        if(WHEEL_ZOOM_ENABLED) 
        {
            aMap.setZoom(aMap.getZoom() - pEvent.getWheelRotation(), pEvent.getPoint());
        }
    }

    /**
     * Sets the mouse button that is used for moving the map. Possible values
     * are:
     * <ul>
     * <li>{@link MouseEvent#BUTTON1} (left mouse button)</li>
     * <li>{@link MouseEvent#BUTTON2} (middle mouse button)</li>
     * <li>{@link MouseEvent#BUTTON3} (right mouse button)</li>
     * </ul>
     *
     * @param pMovementMouseButton The button of choice.
     */
    public void setMovementMouseButton(int pMovementMouseButton) 
    {
        aMovementMouseButton = pMovementMouseButton;
        switch (pMovementMouseButton) 
        {
            case MouseEvent.BUTTON1:
                aMovementMouseButtonMask = MouseEvent.BUTTON1_DOWN_MASK;
                break;
            case MouseEvent.BUTTON2:
                aMovementMouseButtonMask = MouseEvent.BUTTON2_DOWN_MASK;
                break;
            case MouseEvent.BUTTON3:
                aMovementMouseButtonMask = MouseEvent.BUTTON3_DOWN_MASK;
                break;
            default:
                throw new RuntimeException("Unsupported button");
        }
    }

    @Override // Does nothing on purpose
    public void mouseEntered(MouseEvent pEvent) 
    {}

    @Override // Does nothing on purpose
    public void mouseExited(MouseEvent pEvent) 
    {}

    @Override
    public void mouseMoved(MouseEvent pEvent) 
    {
        // Mac OSX simulates with  ctrl + mouse 1  the second mouse button hence no dragging events get fired.
        if (isPlatformOsx())
        {
            if (!MOVEMENT_ENABLED || !aIsMoving)
            {
                return;
            }
            // Is only the selected mouse button pressed?
            if (pEvent.getModifiersEx() == MouseEvent.CTRL_DOWN_MASK) 
            {
                Point p = pEvent.getPoint();
                if (aLastDragPoint != null) 
                {
                    int diffx = aLastDragPoint.x - p.x;
                    int diffy = aLastDragPoint.y - p.y;
                    aMap.moveMap(diffx, diffy);
                }
                aLastDragPoint = p;
            }
        }
    }

    private static boolean isPlatformOsx() 
    {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().startsWith("mac os x");
    }
}
