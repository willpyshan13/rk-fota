package android.rockchip.update.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class XMLUtil {

	// constructor
	public XMLUtil() {

	}

	/**
	 * Getting XML from URL making HTTP request
	 * @param url string
	 * */
	public static String getXmlFromUrl(String url) {
		String xml = null;
		try {
			// defaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			xml = EntityUtils.toString(httpEntity,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// return XML
		return xml;
	}

	/**
	 * Getting XML DOM element
	 * @param XML string
	 * */
	public static Document getDomElement(String xml){
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {

			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			doc = db.parse(is); 

		} catch (ParserConfigurationException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		} catch (SAXException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		}

		return doc;
	}

	/** Getting node value
	 * @param elem element
	 */
	public static String getElementValue( Node elem ) {
		Node child;
		if( elem != null){
			if (elem.hasChildNodes()){
				for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
					if( child.getNodeType() == Node.TEXT_NODE  ){
						return child.getNodeValue();
					}
				}
			}
		}
		return "";
	}
	/**
	 * Getting node value
	 * @param Element node
	 * @param key string
	 * */
	public static String getValue(Element item, String str) {        
		NodeList n = item.getElementsByTagName(str); 
		String value = getElementValue(n.item(0));
		return value == null ? value:value.trim();
	}
	
	
	public static Element getElementByName(Element parentE ,String tagName,String checkName)
	{
		if(parentE == null 
				|| tagName == null || tagName.length()==0 
				|| checkName == null || checkName.length() == 0 )
			return null;
		
		NodeList list = parentE.getElementsByTagName(tagName);
		if(list == null || list.getLength() == 0)return null;
		
		for (int i = 0; i < list.getLength(); i++) { 
			Element element = (Element) list.item(i);
			String curName = element.getAttribute("name");
			Log.v("sjf","getElementByName tagName="+tagName 
					+ "   ,checkName="+checkName+"   ,curName="+curName);
			if(checkName.equals(curName))
			{
				return element;
			}
		}
		
		return null;
	}
}

