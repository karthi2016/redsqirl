package idm.auth;



import idm.BaseBean;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import ch.ethz.ssh2.Connection;


/** UserInfoBean
 * 
 * Class/bean to control user permission and user authentication
 * 
 * @author Igor.Souza
 */
public class UserInfoBean extends BaseBean implements Serializable {

	private static Logger logger = Logger.getLogger(UserInfoBean.class);

	private String userName;
	private String password;
	private String msnError;
	private String msnLoginTwice;
	private boolean loginChek;
	private String twoLoginChek;

	private static ServerThread th;
	private static int port = 2001;

	private static Registry registry;

	private static Connection conn;

	private long currentValue;
	private boolean enabled;

	public UserInfoBean() {

	}

	/** Login
	 * 
	 * Method to validate permission of the user. Receives as input the login and password of the user.
	 * 
	 * @return String - success or failure
	 * @author Igor.Souza
	 */
	public String login() {

		logger.info("login: "+userName);

		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
		ServletContext sc = (ServletContext) fCtx.getExternalContext().getContext();
		Map<String, HttpSession> sessionLoginMap = (Map<String, HttpSession>) sc.getAttribute("sessionLoginMap");

		try {

			String hostname = "localhost";

			conn = new Connection(hostname);
			conn.connect();

			boolean isAuthenticated = conn.authenticateWithPassword(userName, password);

			if (isAuthenticated == false){
				setMsnError("error");
				setTwoLoginChek(null);

				logger.info("Authentication Error");

				return "failure";
			}

			HttpSession sessionLogin = sessionLoginMap.get(userName);

			if(sessionLogin != null && !sessionLogin.getId().equals(session.getId())){
				sessionLoginMap.remove(userName);
				setTwoLoginChek(null);
				sessionLogin.invalidate();

				logger.info("Change Session");

			}

			session.setAttribute("username", userName);
			sessionLoginMap.put(userName, session);
			sc.setAttribute("sessionLoginMap", sessionLoginMap);

			logger.info("Authentication Success");

			setConn(conn);

			setCurrentValue(getCurrentValue()+3);

			//error with rmi connection
			if(!createRegistry(userName, password)){
				getBundleMessage("error.rmi.connection");
				invalidateSession();
				return "failure";
			}

			setCurrentValue(getCurrentValue()+5);

			setMsnError(null);
			return "success";

		} catch (IOException e) {

			logger.error(e.getMessage());
			invalidateSession();
			setMsnError("error");
			return "failure";
		}

	}
	
	private void invalidateSessionReLogin(){

		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);

		ServletContext sc = (ServletContext) fCtx.getExternalContext().getContext();
		sc.setAttribute("userName", userName);
		
		logger.info("before invalidade session");
		session.invalidate();
		logger.info("after invalidade session");
	}

	public String signOutReLogin(){

		logger.info("signOutReLogin");

		//invalidateSessionReLogin();

		return "reStart";
	}
	
	public String reStart(){

		logger.info("reStart");

		return "success";
	}
	
	
	public String reLogin() {

		FacesContext fCtx = FacesContext.getCurrentInstance();
		
		ServletContext scOld = (ServletContext) fCtx.getExternalContext().getContext();
		
		if(scOld.getAttribute("UserInfo") != null && password == null){
			password = ((UserInfo)scOld.getAttribute("UserInfo")).getPassword();
		}
		
		if(scOld.getAttribute("userName") != null && userName == null){
			userName = (String) scOld.getAttribute("userName");
		}
		
		invalidateSessionReLogin();
		
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(true);
		ServletContext sc = (ServletContext) fCtx.getExternalContext().getContext();
		Map<String, HttpSession> sessionLoginMap = (Map<String, HttpSession>) sc.getAttribute("sessionLoginMap");
		
		logger.info("reLogin: " + userName);

		String hostname = "localhost";

		HttpSession sessionLogin = sessionLoginMap.get(userName);

		if(sessionLogin != null && !sessionLogin.getId().equals(session.getId())){
			sessionLoginMap.remove(userName);
			setTwoLoginChek(null);
			sessionLogin.invalidate();

			logger.info("Change Session");

		}

		session.setAttribute("username", userName);
		sessionLoginMap.put(userName, session);
		sc.setAttribute("sessionLoginMap", sessionLoginMap);

		logger.info("Authentication Success");

		//error with rmi connection
		if(!createRegistry(userName, password)){
			getBundleMessage("error.rmi.connection");
			invalidateSession();
			return "failure";
		}

		HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
		request.setAttribute("msnReLogin", "msnReLogin");
		
		setMsnError(null);
		return "reStart";
	}


	/** createRegistry
	 * 
	 * Method to create the connection to the server rmi.
	 * Retrieve objects and places them in the context of the application
	 * 
	 * @return
	 * @author Igor.Souza
	 */
	public boolean createRegistry(String user,String password){

		logger.info("createRegistry");

		List<String> beans = new ArrayList<String>();
		beans.add("wfm");
		beans.add("hive");
		beans.add("ssharray");
		beans.add("oozie");
		beans.add("hdfs");
		beans.add("pckmng");

		FacesContext fCtx = FacesContext.getCurrentInstance();
		ServletContext sc = (ServletContext) fCtx.getExternalContext().getContext();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
		
		try{
			
			th = new ServerThread(port);
			Session s = th.run(user,password);
			
			if(s != null){
				sc.setAttribute("UserInfo", s.getUserInfo());
			}

			registry = LocateRegistry.getRegistry(port);

			setCurrentValue(getCurrentValue()+1);

			session.setAttribute("serverThread", th);
			sc.setAttribute("registry", registry);

			for (String beanName : beans){

				logger.info("createRegistry - " + beanName);
				setCurrentValue(getCurrentValue()+2);

				boolean error = true;
				int cont = 0;

				while(error){
					cont++;
					try{
						Remote dfi = registry.lookup(user+"@"+beanName);
						error = false;
						session.setAttribute(beanName, dfi);
						setCurrentValue(getCurrentValue()+1);
					}catch(Exception e){
						Thread.sleep(500);
						logger.error(e.getMessage());
						setCurrentValue(getCurrentValue()+1);

						if(cont > 20){
							throw e;
						}
					}
				}
			}

			return true;

		}catch(Exception e){
			logger.error("Fail to initialise registry, Exception: "+e.getMessage());
			return false;
		}

	}


	/** validateSecondLogin
	 * 
	 * Method to validate permission of the user.
	 * 
	 * @return String - success or failure
	 * @author Igor.Souza
	 */
	public String validateSecondLogin() {

		FacesContext fCtx = FacesContext.getCurrentInstance();
		ServletContext sc = (ServletContext) fCtx.getExternalContext().getContext();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
		Map<String, HttpSession> sessionLoginMap = (Map<String, HttpSession>) sc.getAttribute("sessionLoginMap");

		HttpSession sessionLogin = sessionLoginMap.get(userName);
		if(sessionLogin != null){

			if(sessionLogin.getId().equals(session.getId())){
				setTwoLoginChek(null);
				setMsnLoginTwice("twice");

				logger.info("Already Authenticated");
				return "failure";
			}

			setTwoLoginChek("two");
			logger.info("Already Authenticated");
			return "failure";
		}

		setCurrentValue(getCurrentValue()+5);

		setTwoLoginChek(null);
		String aux = login();

		return aux;
	}


	/** signOut
	 * 
	 * Method to logs out user of the application. Removes data so User context and removes the session
	 * 
	 * @return string - to navigation
	 * @author Igor.Souza
	 */
	public String signOut(){

		logger.info("signOut");

		invalidateSession();

		return "signout";
	}

	private void invalidateSession(){

		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);

		logger.info("before invalidade session");
		session.invalidate();
		logger.info("after invalidade session");
	}

	public String startProcess() {

		setEnabled(true);
		setCurrentValue(Long.valueOf(10));

		return null;
	}


	/** cleanSession
	 * 
	 * Method to clean all Session and Context
	 * 
	 * @return
	 * @author Igor.Souza
	 */

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getMsnError() {
		return msnError;
	}

	public void setMsnError(String msnError) {
		this.msnError = msnError;
	}

	public boolean isLoginChek() {
		return loginChek;
	}

	public void setLoginChek(boolean loginChek) {
		this.loginChek = loginChek;
	}

	public String getTwoLoginChek() {
		return twoLoginChek;
	}

	public void setTwoLoginChek(String twoLoginChek) {
		this.twoLoginChek = twoLoginChek;
	}

	public String getMsnLoginTwice() {
		return msnLoginTwice;
	}

	public void setMsnLoginTwice(String msnLoginTwice) {
		this.msnLoginTwice = msnLoginTwice;
	}

	public static Connection getConn() {
		return conn;
	}

	public static void setConn(Connection conn) {
		UserInfoBean.conn = conn;
	}

	public long getCurrentValue() {
		return currentValue;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setCurrentValue(long currentValue) {
		this.currentValue = currentValue;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}