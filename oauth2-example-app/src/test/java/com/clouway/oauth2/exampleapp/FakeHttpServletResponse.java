package com.clouway.oauth2.exampleapp;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Stefanov <ivan.stefanov@clouway.com>
 */
public class FakeHttpServletResponse implements HttpServletResponse {
  private final Map<String, Cookie> cookies = new HashMap<String, Cookie>();

  private String lastRedirection = "";

  public void assertContains(Cookie cookie) {
    assertTrue("This cookie does not exist!", cookies.containsKey(cookie.getName()));
    assertTrue("Different cookie value", cookie.getValue().equals(cookies.get(cookie.getName()).getValue()));
  }

  public void assertDoesNotExist(String cookie) {
    assertFalse(cookies.containsKey(cookie));
  }

  public void assertRedirectTo(String location) {
    assertTrue(location.equals(lastRedirection));
  }

  @Override
  public void addCookie(Cookie cookie) {
    cookies.put(cookie.getName(), cookie);
  }

  @Override
  public boolean containsHeader(String name) {
    return false;
  }

  @Override
  public String encodeURL(String url) {
    return null;
  }

  @Override
  public String encodeRedirectURL(String url) {
    return null;
  }

  @Override
  public String encodeUrl(String url) {
    return null;
  }

  @Override
  public String encodeRedirectUrl(String url) {
    return null;
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {

  }

  @Override
  public void sendError(int sc) throws IOException {

  }

  @Override
  public void sendRedirect(String location) throws IOException {
    lastRedirection = location;
  }

  @Override
  public void setDateHeader(String name, long date) {

  }

  @Override
  public void addDateHeader(String name, long date) {

  }

  @Override
  public void setHeader(String name, String value) {

  }

  @Override
  public void addHeader(String name, String value) {

  }

  @Override
  public void setIntHeader(String name, int value) {

  }

  @Override
  public void addIntHeader(String name, int value) {

  }

  @Override
  public void setStatus(int sc) {

  }

  @Override
  public void setStatus(int sc, String sm) {

  }

  @Override
  public int getStatus() {
    return 0;
  }

  @Override
  public String getHeader(String name) {
    return null;
  }

  @Override
  public Collection<String> getHeaders(String name) {
    return null;
  }

  @Override
  public Collection<String> getHeaderNames() {
    return null;
  }

  @Override
  public String getCharacterEncoding() {
    return null;
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return null;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return null;
  }

  @Override
  public void setCharacterEncoding(String charset) {

  }

  @Override
  public void setContentLength(int len) {

  }

  @Override
  public void setContentLengthLong(long l) {

  }

  @Override
  public void setContentType(String type) {

  }

  @Override
  public void setBufferSize(int size) {

  }

  @Override
  public int getBufferSize() {
    return 0;
  }

  @Override
  public void flushBuffer() throws IOException {

  }

  @Override
  public void resetBuffer() {

  }

  @Override
  public boolean isCommitted() {
    return false;
  }

  @Override
  public void reset() {

  }

  @Override
  public void setLocale(Locale loc) {

  }

  @Override
  public Locale getLocale() {
    return null;
  }
}