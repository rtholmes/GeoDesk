/*******************************************************************************
 * GeoDesk - Desktop application to view and edit geographic markers
 *
 *     Copyright (C) 2014 Martin P. Robillard, Jan Peter Stotz, and others
 *     
 *     See: http://martinrobillard.com/geodesk
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.openstreetmap.gui.persistence;

import java.io.File;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility class to load the marker data from file.
 * 
 * @author Martin P. Robillard
 */
public final class KMLReader 
{
	private KMLReader()
	{}
	
    /**
     * Loads the marker data from file.
     * @param pInput The name of the source file.
     * @return An array of markerdata loaded from file.
     * @throws Exception If there's any problem with the load. 
     */
    public static MarkerData[] extractData(String pInput) throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(pInput));
        NodeList lPlacemarks = doc.getElementsByTagName("Placemark");
        MarkerData[] lReturn = new MarkerData[lPlacemarks.getLength()];
        for( int i = 0; i < lPlacemarks.getLength(); i++ )
        {
            Node lPlacemark = lPlacemarks.item(i);
            NodeList lChildren = lPlacemark.getChildNodes();
            lReturn[i] = new MarkerData();
            for( int j = 0; j < lChildren.getLength(); j++)
            {
                Node lNode = lChildren.item(j);
                if( lNode.getNodeType() == Node.ELEMENT_NODE )
                {
                    if( lNode.getNodeName().equals("name"))
                    {
                        Node lTextNode = lNode.getFirstChild();
                        if( lTextNode != null )
                        {
                            lReturn[i].aName = lTextNode.getNodeValue();
                        }
                    }
                    else if( lNode.getNodeName().equals("description"))
                    {
                        Node lTextNode = lNode.getFirstChild();
                        if( lTextNode != null )
                        {
                            lReturn[i].aDescription = cleanDescription(lTextNode.getNodeValue());
                        }
                    }
                    else if( lNode.getNodeName().equals("Point"))
                    {
                        Node lCoordsNode = lNode.getFirstChild().getNextSibling();
                        String[] lCoords = lCoordsNode.getFirstChild().getNodeValue().split(",");
                        lReturn[i].aLongitude = Double.parseDouble(lCoords[0]);
                        lReturn[i].aLatitude = Double.parseDouble(lCoords[1]);
                    }
                }
            }
        }
        return lReturn;
    }
    
    private static String cleanDescription(String pRawDescription)
    {
        Pattern lDiv = Pattern.compile("<div[^>]*?>|</div>");
        String lReturn = lDiv.matcher(pRawDescription).replaceAll("");
        Pattern lBr = Pattern.compile("<br>");
        lReturn = lBr.matcher(lReturn).replaceAll("\n");
        return lReturn.trim();
    }
}
