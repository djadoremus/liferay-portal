/**
 * Copyright (c) 2000-2006 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet;

import com.liferay.portal.model.Layout;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.spring.PortletLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CollectionFactory;
import com.liferay.util.Validator;
import com.liferay.util.servlet.URLEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.lang.reflect.Constructor;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletModeException;
import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;
import javax.portlet.WindowStateException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="RenderResponseImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 *
 */
public class RenderResponseImpl implements RenderResponse {

	public void addProperty(String key, String value) {
	}

	public void setProperty(String key, String value) {
		if (_properties == null) {
			_properties = CollectionFactory.getHashMap();
		}

		_properties.put(key, new String[] {value});
	}

	public PortletURL createPortletURL(boolean action) {
		return createPortletURL(_portletName, action);
	}

	public PortletURL createPortletURL(String portletName, boolean action) {

		// Wrap portlet URL with a custom wrapper if and only if a custom
		// wrapper for the portlet has been defined

		Portlet portlet = getPortlet();

		String portletURLClass = portlet.getPortletURLClass();

		if (Validator.isNotNull(portletURLClass)) {
			try {
				Class portletURLClassObj = Class.forName(portletURLClass);

				Constructor constructor = portletURLClassObj.getConstructor(
					new Class[] {
						com.liferay.portlet.RenderResponseImpl.class,
						boolean.class
					});

				return (PortletURL)constructor.newInstance(
					new Object[] {this, new Boolean(action)});
			}
			catch (Exception e) {
				_log.error(e);
			}
		}

		return new PortletURLImpl(_req, portletName, _plid, action);
	}

	public PortletURL createActionURL() {
		return createActionURL(_portletName);
	}

	public PortletURL createActionURL(String portletName) {
		PortletURL portletURL = createPortletURL(portletName, true);

		try {
			portletURL.setWindowState(_req.getWindowState());
		}
		catch (WindowStateException wse) {
		}

		try {
			portletURL.setPortletMode(_req.getPortletMode());
		}
		catch (PortletModeException pme) {
		}

		return portletURL;
	}

	public PortletURL createRenderURL() {
		return createRenderURL(_portletName);
	}

	public PortletURL createRenderURL(String portletName) {
		PortletURL portletURL = createPortletURL(portletName, false);

		try {
			portletURL.setWindowState(_req.getWindowState());
		}
		catch (WindowStateException wse) {
		}

		try {
			portletURL.setPortletMode(_req.getPortletMode());
		}
		catch (PortletModeException pme) {
		}

		return portletURL;
	}

	public String getNamespace() {
		return PortalUtil.getPortletNamespace(_portletName);
	}

	public void setURLEncoder(URLEncoder urlEncoder) {
		_urlEncoder = urlEncoder;
	}

	public String encodeURL(String path) {
		if ((path == null) ||
			(!path.startsWith("#") && !path.startsWith("/") &&
				(path.indexOf("://") == -1))) {

			// Allow '#' as well to workaround a bug in Oracle ADF 10.1.3

			throw new IllegalArgumentException(
				"URL path must start with a '/' or include '://'");
		}

		if (_urlEncoder != null) {
			return _urlEncoder.encodeURL(path);
		}
		else {
			return path;
		}
	}

	public String getCharacterEncoding() {
		return _res.getCharacterEncoding();
	}

	public String getContentType() {
		return _contentType;
	}

	public void setContentType(String contentType) {
		if (Validator.isNull(contentType)) {
			throw new IllegalArgumentException();
		}

		Enumeration enu = _req.getResponseContentTypes();

		boolean valid = false;

		while (enu.hasMoreElements()) {
			String resContentType = (String)enu.nextElement();

			if (contentType.startsWith(resContentType)) {
				valid = true;
			}
		}

		if (!valid) {
			throw new IllegalArgumentException();
		}

		_contentType = contentType;
	}

	public Locale getLocale() {
		return _req.getLocale();
	}

	public OutputStream getPortletOutputStream() throws IOException{
		if (_calledGetWriter) {
			throw new IllegalStateException();
		}

		if (_contentType == null) {
			throw new IllegalStateException();
		}

		_calledGetPortletOutputStream = true;

		return _res.getOutputStream();
	}

	public String getTitle() {
		return _title;
	}

	public void setTitle(String title) {
		_title = title;
	}

	public Boolean getUseDefaultTemplate() {
		return _useDefaultTemplate;
	}

	public void setUseDefaultTemplate(Boolean useDefaultTemplate) {
		_useDefaultTemplate = useDefaultTemplate;
	}

	public PrintWriter getWriter() throws IOException {
		if (_calledGetPortletOutputStream) {
			throw new IllegalStateException();
		}

		if (_contentType == null) {
			throw new IllegalStateException();
		}

		_calledGetWriter = true;

		return _res.getWriter();
	}

	public int getBufferSize() {
		return _res.getBufferSize();
	}

	public void setBufferSize(int size) {
		_res.setBufferSize(size);
	}

	public void flushBuffer() throws IOException {
		_res.flushBuffer();
	}

	public void resetBuffer() {
		_res.resetBuffer();
	}

	public boolean isCommitted() {
		return false;
	}

	public void reset() {
	}

	public HttpServletResponse getHttpServletResponse() {
		return _res;
	}

	public Portlet getPortlet() {
		if (_portlet == null) {
			try {
				_portlet = PortletLocalServiceUtil.getPortletById(
					_companyId, _portletName);
			}
			catch (Exception e) {
				_log.error(e);
			}
		}

		return _portlet;
	}

	protected RenderResponseImpl() {
		if (_log.isDebugEnabled()) {
			_log.debug("Creating new instance " + hashCode());
		}
	}

	protected void init(
		RenderRequestImpl req, HttpServletResponse res, String portletName,
		String companyId, String plid) {

		_req = req;
		_res = res;
		_portletName = portletName;
		_companyId = companyId;
		setPlid(plid);
	}

	protected void recycle() {
		if (_log.isDebugEnabled()) {
			_log.debug("Recycling instance " + hashCode());
		}

		_req = null;
		_res = null;
		_portletName = null;
		_portlet = null;
		_companyId = null;
		_plid = null;
		_urlEncoder = null;
		_title = null;
		_useDefaultTemplate = null;
		_contentType = null;
		_calledGetPortletOutputStream = false;
		_calledGetWriter = false;
	}

	protected RenderRequestImpl getReq() {
		return _req;
	}

	protected String getPortletName() {
		return _portletName;
	}

	protected String getCompanyId() {
		return _companyId;
	}

	protected String getPlid() {
		return _plid;
	}

	protected void setPlid(String plid) {
		_plid = plid;

		if (_plid == null) {
			Layout layout = (Layout)_req.getAttribute(WebKeys.LAYOUT);

			if (layout != null) {
				_plid = layout.getPlid();
			}
		}
	}

	protected Map getProperties() {
		return _properties;
	}

	protected URLEncoder getUrlEncoder() {
		return _urlEncoder;
	}

	protected boolean isCalledGetPortletOutputStream() {
		return _calledGetPortletOutputStream;
	}

	protected boolean isCalledGetWriter() {
		return _calledGetWriter;
	}

	private static Log _log = LogFactory.getLog(RenderRequestImpl.class);

	private RenderRequestImpl _req;
	private HttpServletResponse _res;
	private String _portletName;
	private Portlet _portlet;
	private String _companyId;
	private String _plid;
	private Map _properties;
	private URLEncoder _urlEncoder;
	private String _title;
 	private Boolean _useDefaultTemplate;
	private String _contentType;
	private boolean _calledGetPortletOutputStream;
 	private boolean _calledGetWriter;

}