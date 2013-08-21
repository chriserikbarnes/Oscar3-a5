package uk.ac.cam.ch.wwmm.oscar3server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Serializer;
import nu.xom.Text;

/**Experimental: Wraps a piece of XML in an envelope and sends it back to an Ajax client.
 * 
 * @author ptc24
 *
 */
public class AjaxResponse {

	public static void doAjaxResponse(HttpServletRequest request, HttpServletResponse response,
			String output) throws Exception {
		doAjaxResponse(request, response, new Text(output));
	}
	
	public static void doAjaxResponse(HttpServletRequest request, HttpServletResponse response,
			Node output) throws Exception {
		String function = request.getParameter("function");
		response.setContentType("application/xml");
		Element responseElem = new Element("response");
		Document responseDoc = new Document(responseElem);
		Element functionElem = new Element("function");
		functionElem.appendChild(function);
		responseElem.appendChild(functionElem);
		Element resultElem = new Element("result");
		resultElem.appendChild(output);
		responseElem.appendChild(resultElem);
		new Serializer(response.getOutputStream()).write(responseDoc);
	}
	
}
