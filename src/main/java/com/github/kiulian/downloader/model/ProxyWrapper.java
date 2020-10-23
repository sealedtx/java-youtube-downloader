package com.github.kiulian.downloader.model;

import org.apache.commons.codec.binary.Base64;

import java.net.InetSocketAddress;
import java.net.URLConnection;

/**
 * Proxy Wrapper
 */
public class ProxyWrapper {

	public String host;

	public int port;

	public String username;

	public String password;

	public ProxyWrapper(String host, int port) {
		this(host, port, null, null);
	}

	public ProxyWrapper(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public java.net.Proxy toProxy() {
		InetSocketAddress address = new InetSocketAddress(host, port);
		return new java.net.Proxy(java.net.Proxy.Type.HTTP, address);
	}

	private String getAuthenticationHeader() {
		String authParam = username + ":" + password;
		authParam = new String(Base64.encodeBase64(authParam.getBytes()));
		return "Basic " + authParam;
	}

	public void setConn(URLConnection conn) {

		if(username != null && password != null) {
			String headerKey = "Proxy-Authorization";
			conn.addRequestProperty(headerKey, getAuthenticationHeader());
		}
	}
}
