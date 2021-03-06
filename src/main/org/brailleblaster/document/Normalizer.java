package org.brailleblaster.document;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.brailleblaster.util.Notify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class Normalizer {
	File f;
	Document doc;
	final static Logger log = LoggerFactory.getLogger(Normalizer.class);
	String originalPubId;
	String originalSystemId;
	DocumentType docType;
	Resolver res;
	
	public Normalizer(BBDocument bbDoc, String path){
		this.f = new File(path);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		res = new Resolver();
		
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.setEntityResolver(res);
			this.doc = dBuilder.parse(this.f);
			docType = this.doc.getDoctype();
			bbDoc.setPublicId(res.getOriginalpubId());
			bbDoc.setSystemId(res.getOriginalSystemId());
		}
		catch(ConnectException e){
			new Notify("Brailleblaster failed to access necessary materials from online.  Please check your internet connection and try again.");
			e.printStackTrace();
			log.error("Connections Error", e);
		}
		catch(UnknownHostException e){
			new Notify("Brailleblaster failed to access necessary materials from online.  Please check your internet connection and try again.");
			e.printStackTrace();
			log.error("Unknown Host Error", e);
		}
		catch (ParserConfigurationException e) {
			new Notify("An error occurred while reading the document. Please check whehther the document contains vaild XML.");
			e.printStackTrace();
			log.error("Parse Error", e);
		}
		catch (SAXException e) {
			new Notify("An error occurred while reading the document. Please check whehther the document contains vaild XML.");
			e.printStackTrace();
			log.error("Sax Error", e);
		} 
		catch (IOException e) {
			new Notify("An error occurred while reading the document.");
			e.printStackTrace();
			log.error("IO Error", e);
		}
	}
	
	public boolean createNewNormalizedFile(String path){
		if(this.doc != null){
			normalize();
			return write(path);
		}
		
		return false;
	}
	
	private void normalize(){
		doc.normalize();
		removeEscapeChars(doc.getDocumentElement());
	}
	
	private void removeEscapeChars(Element e){
		NodeList list = e.getChildNodes();
		
		for(int i = 0; i < list.getLength(); i++){
			if(list.item(i) instanceof Element){
				removeEscapeChars((Element)list.item(i));
			}
		
			
			else if(list.item(i) instanceof Text){
				Text t = (Text)list.item(i);
				
				String text = t.getTextContent();
				if(t.getParentNode().getNodeName().equals("pagenum")){
					// Remove word page from pagenum element
					text=cleanPageNum(text);
					
					
				}
				if(text.length() > 0 && text.charAt(0) == '\n' && onlyWhitespace(text))
					text = text.replaceAll("\\s+", "");
				else if(text.length() > 0 && (text.charAt(0) == '\n' || text.charAt(0) == '\t')){
					text = text.trim();
					text = text.replaceAll("\\s+", " ");
				}
				else
					text = text.replaceAll("\\s+", " ");
				
				t.setTextContent(text);
			}
		}
	}
	
	
	private boolean onlyWhitespace(String text){
		for(int j = 0; j < text.length(); j++){
			if(!Character.isWhitespace(text.charAt(j)))
				return false;
		}
		return true;
	}
	
	public boolean write(String path) {
		URL dtdURL = null;
		if (res.dtdName != null)
		{
			// First see if we have a URL
			try
			{
				dtdURL = new URL(res.dtdName);
			}
			catch (MalformedURLException e)
			{
				// Don't do anything here
			}
			// If dtdURL is not yet assigned we will assume file path, can we do better?
			if (dtdURL == null)
			{
				try
				{
					dtdURL = new File(res.dtdName).toURI().toURL();
				}
				catch (MalformedURLException e)
				{
					// Do nothing
				}
			}
		}
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;
			transformer = transformerFactory.newTransformer();
			if(dtdURL != null)
				transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, dtdURL.toString());
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(path));
			transformer.transform(source, result);
			return true;
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			log.error("Transformer Configuration Exception", e);
			return false;
		}
		catch (TransformerException e) {
			e.printStackTrace();
			log.error("Transformer Exception", e);
			return false;
		}
		catch(Exception e){
			e.printStackTrace();
			log.error("Unforeseen Exception", e);
			return false;
		}
    }
	
	
	//Remove word page from pagenum element
		private String cleanPageNum(String str){
			int startRemove =0;
			int endRemove=0;
			if((str.toLowerCase().contains("page"))){
				startRemove=str.toLowerCase().indexOf("p");
				endRemove=str.toLowerCase().indexOf("e");
				String removedString=str.substring(startRemove, endRemove+1);
				str=str.replace(removedString, "");
							}
			return str.trim();
			
		}
}
