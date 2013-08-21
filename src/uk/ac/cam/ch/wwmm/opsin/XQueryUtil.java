package uk.ac.cam.ch.wwmm.opsin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nu.xom.Node;
import nu.xom.Nodes;

/**
 * Checks for the presence of Nux using reflection.
 * If it is available Nux is used for XQueries, otherwise XOM's XQuery is used as a fall back.
 * This allows Nux/Saxon to be optional dependencies
 * @author dl387
 *
 */
public class XQueryUtil {

	private static XQueryUtil myInstance = null;
	private Method xqueryMethod;

	static Nodes xquery(Node node, String xquery){
		XQueryUtil xqu =getInstance();
		if (xqu.xqueryMethod !=null){
			try{
				Object result =xqu.xqueryMethod.invoke(null, new Object[] {node, xquery});
				return (Nodes)result;
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		else{
			return node.query(xquery);
		}
	}

	@SuppressWarnings("unchecked")
	public XQueryUtil() {
		try{
			Class xqueryUtil = Class.forName("nux.xom.xquery.XQueryUtil");
			xqueryMethod =xqueryUtil.getMethod("xquery", new Class[] {nu.xom.Node.class, String.class });
		}
		catch(Exception e){
			xqueryMethod =null;
		}
	}
	
	private static XQueryUtil getInstance() {
		if(myInstance == null) myInstance = new XQueryUtil();
		return myInstance;
	}
}
