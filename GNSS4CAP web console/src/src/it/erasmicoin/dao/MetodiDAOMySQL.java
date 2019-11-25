package it.erasmicoin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import javax.lang.model.type.NullType;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import it.erasmicoin.utility.Utility;

public class MetodiDAOMySQL extends DaoManager {
	
	public static Logger logger = Logger.getLogger(MetodiDAOMySQL.class);
	
	public static String USER_TYPE_SUPERUSER = "SUPERUSER";
	public static String USER_TYPE_PAYING_AGENCY = "PAYING_AGENCY";
	public static String USER_TYPE_USER = "USER";
	
	public JSONArray getAgencyAdmins() throws Exception{
		JSONArray usersList = new JSONArray();
		
		String sql = "select a.id_user, a.user " + 
				" from users a, user_roles b, roles c " + 
				"where a.id_user = b.id_user " + 
				"  and b.id_role = c.id_role " + 
				"  and a.data_fine is null " + 
				"  and b.data_fine is null " + 
				"  and c.ruolo = 'PAYING_AGENCY'";
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try{
			conn = getConnectionMySql();
			logger.debug(sql);
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			
			//For users with no affiliation
			JSONObject userItem = new JSONObject();
			userItem.put("id", "0");
			userItem.put("user", "None");
			usersList.put(userItem);
			
			while(rs.next()) {
				userItem = new JSONObject();
				userItem.put("id",rs.getLong("id_user"));
				userItem.put("user", rs.getString("user"));
				usersList.put(userItem);
			}
			
			return usersList;
		} catch (Exception e) {
			logger.error("error in getAgencyAdmins",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
	}
	
	public JSONObject getUsers(String token) throws Exception{
		
		JSONObject retObj = new JSONObject();
		
		JSONArray usersList = new JSONArray();
		
		//String sqlAdmin = "SELECT id_user, user, data_inizio, data_fine FROM `users`";
		/*String sqlAdmin = "select users.*, user_roles.id_role, roles.ruolo"
					+ " from users left join (user_roles, roles) on (users.id_user = user_roles.id_user and user_roles.id_role = roles.id_role)";*/
		String sqlAdmin = "SELECT userWithRole.*, affiliationusers.user as affiliation, affiliationusers.id_user as idAffiliation " + 
				"from (select users.*, user_roles.id_role, roles.ruolo, roles.role_desc " + 
				"from users left join (user_roles, roles) on (users.id_user = user_roles.id_user and user_roles.id_role = roles.id_role and user_roles.data_fine is null)) userWithRole " + 
				"left join (user_affiliation, users as affiliationusers) on (user_affiliation.id_user = userWithRole.id_user and user_affiliation.id_superuser = affiliationusers.id_user)";
		
		String sqlAgency = "Select id_user, user, data_inizio, data_fine from users a where a.data_fine is null and a.id_user in (select b.id_user from user_affiliation b where b.id_superuser = ?)";
		
		String sql ="";
		logger.debug("getUsers DAO");
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		JSONObject currentUser = getUserFromActiveToken(token);

		if(currentUser.getString("role").equalsIgnoreCase(USER_TYPE_SUPERUSER)) {
			sql = sqlAdmin;
		}else if(currentUser.getString("role").equalsIgnoreCase(USER_TYPE_PAYING_AGENCY)) {
			sql = sqlAgency;
		}
		
		try{
			conn = getConnectionMySql();
			
			logger.debug(sql);
			
			ps = conn.prepareStatement(sql);
			if(currentUser.getString("role").equalsIgnoreCase(USER_TYPE_PAYING_AGENCY)) {
				ps.setLong(1, currentUser.getLong("id"));
			}
			
			rs = ps.executeQuery();
			
			while(rs.next()) {
				JSONObject userItem = new JSONObject();
				userItem.put("id", rs.getString("id_user"));
				userItem.put("user", rs.getString("user"));
				userItem.put("data_inizio", rs.getDate("data_inizio"));
				userItem.put("data_fine", rs.getDate("data_fine"));
				if(currentUser.getString("role").equalsIgnoreCase(USER_TYPE_SUPERUSER)) {
					userItem.put("role", rs.getString("ruolo"));
					userItem.put("roleDesc", rs.getString("role_desc"));
					userItem.put("affiliatedWith", rs.getString("affiliation"));
					userItem.put("idAffiliation", rs.getLong("idAffiliation"));
				}else {
					userItem.put("role", USER_TYPE_USER);
				}
				
				usersList.put(userItem);
			}
			retObj.put("users", usersList);
			if(currentUser.getString("role").equalsIgnoreCase(USER_TYPE_SUPERUSER)) {
				JSONArray admins = getAgencyAdmins();
				retObj.put("admins", admins);
			}
			
			
		} catch (Exception e) {
			logger.error("errore in getUsers",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
		return retObj;
	}
	
	public JSONObject getUserInfo(String username) throws Exception{
				
		String sql = "select a.id_user, c.ruolo, c.role_desc from users a, user_roles b, roles c " + 
				"		where a.id_user = b.id_user " + 
				"		  and a.data_fine is null " + 
				"		  and b.id_role = c.id_role " + 
				"		  and b.data_fine is null " + 
				"		  and c.data_fine is null " + 
				"          and lower(a.user) = lower(?)";
		
		logger.debug("getUsers DAO");
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		JSONObject userInfo = new JSONObject();
		try{
			conn = getConnectionMySql();
			
			logger.debug(sql);
			
			ps = conn.prepareStatement(sql);
			ps.setString(1, username);
			rs = ps.executeQuery();
			
			
			while(rs.next()) {
				userInfo.put("role", rs.getString("ruolo"));
				userInfo.put("roleDesc", rs.getString("role_desc"));
				userInfo.put("id", rs.getLong("id_user"));
			}
			
		} catch (Exception e) {
			logger.error("error in getUserInfo",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
		return userInfo;
		  
	}
	
	public int createUser(String username, String password, String role, Long idParent) throws Exception {
		logger.debug("createUser DAO");
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		int newUserId = 0;
		String sql = "INSERT INTO `users`(`user`, `pwd`) VALUES (?,password(?))";
		
		try{
			conn = getConnectionMySql();
			conn.setAutoCommit(false);
			
			
			ps = conn.prepareStatement(sql,  Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, username);
			ps.setString(2, password);
			
			int rowCount = ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			
			while(rs.next()) {
				newUserId = rs.getInt(1);
			}
			ps.close();
			if(rowCount > 0) {
				sql = "INSERT INTO `user_roles`(`id_user`, `id_role`, `data_inizio`, `data_fine`) VALUES (?,(select id_role from roles where lower(ruolo) = lower(?)),now(), null)";
				ps = conn.prepareStatement(sql);
				ps.setLong(1, newUserId);
				ps.setString(2, role);
			
				rowCount = ps.executeUpdate();
				if(rowCount > 0) {
					if(idParent != null) {
						sql = "INSERT INTO `user_affiliation`(`id_user`, `id_superuser`) VALUES (?,?)";
						ps = conn.prepareStatement(sql);
						ps.setLong(1, newUserId);
						ps.setLong(2, idParent);
						
						rowCount = ps.executeUpdate();
						if(rowCount > 0) {
							conn.commit();
							return newUserId;
						}else {
							conn.rollback();
							throw new Exception("Error in create user - A1");
						}
					}else {
						conn.commit();
						return newUserId;
					}
					
				}else {
					conn.rollback();
					throw new Exception("Error in create user - A4");
				}
				
			}else {
				conn.rollback();
				throw new Exception("Error in create user - A2");
			}
			
		} catch (Exception e) {
			logger.error("Error in create user - A3",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
		
	}
	
	public boolean suspendUser(int userId) throws Exception {
		boolean actionStatus = false;
		logger.debug("suspendUser DAO");
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		String sql = "UPDATE `users` set `data_fine` = NOW() where id_user = ?";
		
		try{
			conn = getConnectionMySql();
			
			
			ps = conn.prepareStatement(sql);
			ps.setInt(1,userId);
			
			int rowCount = ps.executeUpdate();
			if(rowCount > 0)
				actionStatus = true;
			
		} catch (Exception e) {
			logger.error("error in suspendUser",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		return actionStatus;
	}
	
	public boolean activateUser(int userId) throws Exception {
		boolean actionStatus = false;
		logger.debug("activateUser DAO");
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		int newUserId = 0;
		String sql = "UPDATE `users` set `data_fine` = null where id_user = ?";
		
		try{
			conn = getConnectionMySql();
			
			
			ps = conn.prepareStatement(sql);
			ps.setInt(1,userId);
			
			int rowCount = ps.executeUpdate();
			if(rowCount > 0)
				actionStatus = true;
			
		} catch (Exception e) {
			logger.error("error in activateUser",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		return actionStatus;
	}
	
	public boolean updateUser(Long userId, String password, String role, Long idSuper) throws Exception {
		boolean actionStatus = true;
		logger.debug("updateUser DAO");
		
		Connection conn = null;
		PreparedStatement ps = null;
		
		
		/*
		update user_roles set data_fine = now() where id_user = ?
				INSERT INTO `user_roles`(`id_user`, `id_role`, `data_inizio`, `data_fine`) VALUES (?, (select id_role from roles where lower(ruolo) = lower(?), now(), null)
				UPDATE `user_affiliation` SET `id_superuser`=? WHERE id_user = ?
			*/
		
		String sql = "";
		
		try{
			
			conn = getConnectionMySql();
			
			if(!password.isEmpty()) {
				logger.debug("changing password");
				sql = "UPDATE `users` set `pwd` = password(?) where id_user = ?";
				
				ps = conn.prepareStatement(sql);
				ps.setString(1,password);
				ps.setLong(2, userId);
				
				ps.executeUpdate();
				ps.close();
			}
			if(role != null && !role.isEmpty()) {
				logger.debug("updating role");
				sql = "update user_roles set data_fine = now() where id_user = ?";
				
				ps = conn.prepareStatement(sql);
				ps.setLong(1, userId);
				ps.executeUpdate();
				ps.close();
				
				sql = "INSERT INTO `user_roles`(`id_user`, `id_role`, `data_inizio`, `data_fine`) VALUES (?, (select id_role from roles where lower(ruolo) = lower(?)), now(), null)";
				
				ps = conn.prepareStatement(sql);
				ps.setLong(1, userId);
				ps.setString(2, role);
				ps.executeUpdate();
				ps.close();
			}
			logger.debug("removing affiliation");
			sql = "delete from `user_affiliation` WHERE id_user = ?";
			ps = conn.prepareStatement(sql);
			ps.setLong(1, userId);
			
			ps.executeUpdate();
			ps.close();
			
			if(idSuper != null && idSuper > 0) {
				logger.debug("adding affiliation");
				
				sql = "REPLACE INTO user_affiliation(id_user,id_superuser) VALUES(?,?)";
				ps = conn.prepareStatement(sql);
				ps.setLong(1, userId);
				ps.setLong(2,idSuper);
				
				ps.executeUpdate();
				ps.close();
			}
			/*if(idSuper != null && idSuper == 0) {
				logger.debug("removing affiliation");
				sql = "delete from `user_affiliation` WHERE id_user = ?";
				ps = conn.prepareStatement(sql);
				ps.setLong(1, userId);
				
				ps.executeUpdate();
				ps.close();
			}*/
			
		} catch (Exception e) {
			logger.error("error in updateUser",e);
			throw e;
		}
		finally{
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		return actionStatus;
	}
	
	public JSONObject getUserFromActiveToken(String token) throws Exception{

		String sql = "select b.id_user, a.user, d.ruolo, d.role_desc " + 
				"from logins_activity a, users b, user_roles c, roles d " + 
				"where a.user = b.user " + 
				"and b.id_user = c.id_user " + 
				"and c.id_role = d.id_role " + 
				"and c.data_fine is null " + 
				"and d.data_fine is null " + 
				"and b.data_fine is null " + 
				"and a.token = ? " + 
				"and a.valid = 1 " + 
				"and a.loggedout = 0";
		
		JSONObject user = new JSONObject();
		logger.debug("getUserFromActiveToken DAO");
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try{
			conn = getConnectionMySql();
			
			logger.debug(sql);
			
			ps = conn.prepareStatement(sql);
			ps.setString(1, token);
			rs = ps.executeQuery();
			
			
			while(rs.next()) {
				user.put("id", rs.getLong("id_user"));
				user.put("username", rs.getString("user"));
				user.put("role", rs.getString("ruolo"));
				user.put("roleDesc", rs.getString("role_desc"));
			}
			
		} catch (Exception e) {
			logger.error("error in getUserFromActiveToken",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
		return user;
	}
	
	public boolean checkToken(String token, int tokenDurationInMsec) throws Exception{
		
		logger.debug("checkToken DAO");
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean validToken = false;
		try{
			conn = getConnectionMySql();
			
			Integer durationInHours = ((tokenDurationInMsec / 1000) / 60) / 60;
			Integer durationInMinutes = null;
			if(durationInHours < 1) {
				durationInHours = null;
				durationInMinutes =  ((tokenDurationInMsec / 1000) / 60);
			}
				
			
			String sql = "select count(*) validLogin from logins_activity a where token = ? " + 
					"and DATE_ADD(date, INTERVAL "+ ( durationInHours != null ?  + durationInHours+" HOUR" : durationInMinutes + " MINUTE" ) +") >= CURRENT_TIMESTAMP " + 
					"and valid = 1 " +
					"and loggedout = 0 "+
					"and  date = (select max(date) from logins_activity where user = a.user and token = a.token and valid = 1 and loggedout = 0)";
			
			logger.debug(sql);
			
			ps = conn.prepareStatement(sql);
			ps.setString(1, token);
			
			rs = ps.executeQuery();
			
			while(rs.next()) {
				if(rs.getInt(1) > 0) {
					validToken = true;
				}
			}
			
		} catch (Exception e) {
			logger.error("error in checkToken",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		logger.debug("token valid: "+validToken);
		return validToken;
	}
	
	public JSONObject logoutUser(String user) throws Exception{
		Connection conn = null;
		PreparedStatement ps = null;
				
		//close the login row 
		try{
			conn = getConnectionMySql();
			
			String sql = "update logins_activity set loggedout = 1 where user = ? and valid = 1 and loggedout = 0";
			
			ps = conn.prepareStatement(sql);
			ps.setString(1, user);
			
			int rowCount = ps.executeUpdate();
			
			JSONObject ret = new JSONObject();
			
			ret.put("result", true);
			
			return ret; 
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
	}
	
	public JSONObject loginUser(String username, String password, int tokenLength, String fromApp) throws Exception{
		Connection conn = null;
		PreparedStatement ps = null;
				
		boolean isValid = checkUser(username, password);
		String token = "";
		if(isValid) {
			//genero il token
			token = Utility.generateToken(username, tokenLength);
		}
		//salvo il tentativo di login 
		try{
			conn = getConnectionMySql();
			int _fromApp = (fromApp != null ? Integer.parseInt(fromApp) : 0);
			String sql = "UPDATE `logins_activity` SET loggedout = 1 where `user` = ? and valid = 1 and loggedout = 0 and from_app = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, username);
			ps.setInt(2, _fromApp);
			int rowCount = ps.executeUpdate();
			ps.close();
			
			logger.debug("closed "+rowCount+ " previous logins");
			
			logger.debug("backup old logins");
			sql = "INSERT INTO past_logins_activity (SELECT * FROM logins_activity WHERE DATE < (SELECT DATE_SUB(now(),INTERVAL 3 DAY) pastdate))";
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
			ps.close();
			
			logger.debug("deleting old logins");
			sql = "DELETE FROM logins_activity WHERE DATE < (SELECT DATE_SUB(now(),INTERVAL 3 DAY) pastdate)";
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
			ps.close();
			
			logger.debug("inserting new login");
			sql = "INSERT INTO `logins_activity`(`user`, `token`, `valid`,`from_app`) VALUES (?,?,?,?)";
			
			ps = conn.prepareStatement(sql);
			ps.setString(1, username);
			ps.setString(2, token);
			ps.setBoolean(3, isValid);
			ps.setInt(4, _fromApp);
			rowCount = ps.executeUpdate();
			
			JSONObject ret = new JSONObject();
			
			ret.put("result", isValid);
			if(isValid)
				ret.put("token", token);
			
			return ret; 
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
	}
	
	public boolean checkUser(String username, String password) throws Exception {
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean validUser = false;
		
		try{
			conn = getConnectionMySql();
			
			String sql = "SELECT count(*) valid_users FROM `users` WHERE user = ? and pwd = password(?) and data_fine is null";
			
			ps = conn.prepareStatement(sql);
			ps.setString(1, username);
			ps.setString(2, password);
			
			rs = ps.executeQuery();
			
			while(rs.next()) {
				if(rs.getInt(1) > 0) {
					validUser = true;
				}
			}
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}	
		return validUser;
	}
		
	public boolean isSuperUser(String username) throws Exception {
		//
		logger.debug("isSuperUser DAO");
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isSuper = false;
		try{
			conn = getConnectionMySql();
			
			String sql = "SELECT count(*) FROM `users` WHERE user = ? and data_fine is null and superuser = 1";
			
			logger.debug(sql);
			
			ps = conn.prepareStatement(sql);
			ps.setString(1, username);
			
			rs = ps.executeQuery();
			
			while(rs.next()) {
				if(rs.getInt(1) > 0) {
					isSuper = true;
				}
			}
			
		} catch (Exception e) {
			logger.error("error in isSuperUser",e);
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}	
		return isSuper;
	}
	
	public JSONObject getGSAPhoto(String key) throws Exception{
		
		JSONObject obj = new JSONObject();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			
			//String sql = "SELECT `id`, `date`, `json`, `status` FROM `foto_gsa` WHERE `id` = ? and delete_date is null";
			String sql = "select foto_gsa.id, foto_gsa.date, foto_gsa.json, foto_gsa.status, foto_cell_info.resp as cellInfo from foto_gsa LEFT JOIN (foto_cell_info) ON ( foto_gsa.id = foto_cell_info.id_foto) where foto_gsa.delete_date is null and foto_gsa.id = ?";			
			
			conn = getConnectionMySql();
			
			ps = conn.prepareStatement(sql);
			ps.setString(1, key);
			
			rs = ps.executeQuery();
			
			
			String fileName = "";
			while(rs.next()){
						
				obj = new JSONObject(rs.getString("json"));
				
				obj.put("id", rs.getString("id"));
				obj.put("dataUpload", rs.getString("date"));
				
				fileName = rs.getString("id")+"/photo_"+rs.getString("id")+"."+Utility.getStringFromJSONField("ext", obj);
				
				log.error("sto cercando file "+fileName);
				
				String uri_photo = Utility.encodeFileToBase64Binary(Utility.getFolderName(fileName));
				
				if(uri_photo.indexOf("data:image/jpeg;base64,") < 0){
					uri_photo = "data:image/jpeg;base64,"+uri_photo;
				}
				obj.put("uri_photo", uri_photo);
				obj.put("status", rs.getString("status"));
				obj.put("cellInfo", rs.getString("cellInfo"));
			}
										
									
			//ret.put(key, obj);
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		return obj;
	}
	
	public ArrayList<String> getAffiliatedUsers(long idUser) throws Exception {
		
		ArrayList<String> affiliatedUsers = new ArrayList<String>();
		
		String sql = "SELECT b.id_user, b.user FROM `user_affiliation` a, users b where a.id_superuser = ? and b.id_user = a.id_user";
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try{
			conn = getConnectionMySql();
			ps = conn.prepareStatement(sql);
			
			ps.setLong(1, idUser);
			
			rs = ps.executeQuery();
			
			while(rs.next()){
				affiliatedUsers.add(rs.getString("user"));
			}
			return affiliatedUsers;
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
		
	}
	
	
	
	public JSONObject getGSAPhotoList(JSONObject objInput) throws Exception{
		
		JSONObject jsonRet = new JSONObject();
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try{
			conn = getConnectionMySql();
			
			
			String idInput = Utility.getStringFromJSONField("id", objInput);
			String user = Utility.getStringFromJSONField("user", objInput);
			logger.debug("User requiring "+user);
			JSONObject userInfo = getUserInfo(user);
			
			if(user == null || user.isEmpty()) {
				logger.debug("No USER!");
				logger.debug(objInput.toString());
				return null;
			}
			
			String sql = "select foto_gsa.id, foto_gsa.date, foto_gsa.json, foto_gsa.status, foto_cell_info.resp as cellInfo from foto_gsa LEFT JOIN (foto_cell_info) ON ( foto_gsa.id = foto_cell_info.id_foto) where foto_gsa.delete_date is null";		
			
			ArrayList<String> affUsers = null;
			
			if(idInput != null && !idInput.equals(""))
				sql += " and foto_gsa.id = ? ";
			else {
				 affUsers = getAffiliatedUsers(userInfo.getLong("id"));
			}
			sql +=" order by foto_gsa.date asc";
			ps = conn.prepareStatement(sql);
			
			int indice = 1;
			if(idInput != null && !idInput.equals(""))
				ps.setString(indice++, idInput);
			
			rs = ps.executeQuery();
			
			JSONObject obj = new JSONObject();
			JSONArray photosList = new JSONArray();
			
			while(rs.next()){
				
				JSONObject json = new JSONObject(rs.getString("json"));
				obj = new JSONObject();
				obj.put("id", rs.getString("id"));
				obj.put("date", rs.getTimestamp("date"));
				String jsonStr = rs.getString("json");
				obj.put("json", jsonStr);
				String status = rs.getString("status");
				
				obj.put("status", status );
				obj.put("cellInfo", rs.getString("cellInfo"));
				
				if(userInfo.getString("role").equalsIgnoreCase(USER_TYPE_SUPERUSER)) {
					photosList.put(obj);
				}else if(userInfo.getString("role").equalsIgnoreCase(USER_TYPE_PAYING_AGENCY)) {
					for(int i=0;i<affUsers.size(); i++) {
						if(json.has("username") && json.get("username").equals(affUsers.get(i))) {
							photosList.put(obj);
						}
					}
				}else {
					if(json.has("username") && json.get("username").equals(user)) {
						photosList.put(obj);
					}
				}
				
			}
			
			jsonRet.put("photos", photosList);
			
			logger.debug("sending "+photosList.length()+" elements");
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			if(conn!=null) conn.close();
		}
		
		return jsonRet;
	}
	
	public long insertGSAPhoto(JSONObject jsonObjInput) throws Exception{
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		long seqOutput = 0;
		
		try {
			
			jsonObjInput.remove("uri_photo");

			if(jsonObjInput.has("signature"))
				jsonObjInput.remove("signature");
			
			if(jsonObjInput.has("url"))
				jsonObjInput.remove("url");
			
			conn = getConnectionMySql();
			
			String sql = "INSERT INTO `foto_gsa`(`json`) VALUES (?)";
			ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
			
			
			/*
			 * jsonObjInput corrisponde a attributi_foto inviato dall'app
			 */
			ps.setString(1, jsonObjInput.toString());
			
			int rowInserted = ps.executeUpdate();
			
			if(rowInserted > 0){
				rs = ps.getGeneratedKeys();
				while(rs.next()) {
					seqOutput = rs.getLong(1);
				}
				log.error("id inserito :"+seqOutput);
			}
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(ps != null) ps.close();
			if(rs != null) rs.close();
			if(conn != null) conn.close();
		}
		
		return seqOutput;
	}
	
	public void cancellaIdFoto(String idFoto) throws Exception{
		
		Connection conn = null;
		Statement ps = null;
		
		try {
			
			conn = getConnectionMySql();
			
			String sqlUpdate = "update foto_gsa set delete_date = sysdate() where id = "+idFoto;
			
			ps = conn.createStatement();
			ps.executeUpdate(sqlUpdate);
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(ps != null) ps.close();
			if(conn != null) conn.close();
		}
	}
	
	public JSONObject getCellResponse(Long photoId)  throws Exception {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			
			conn = getConnectionMySql();

			String sql = "select * from foto_cell_info where id_foto = ?";
			
			ps = conn.prepareStatement(sql);
			
			int indice = 1;
			
			ps.setLong(indice++, photoId);
			
			
			rs = ps.executeQuery();
			
			JSONObject resp = null;
			while(rs.next()) {
				resp = new JSONObject(rs.getString("resp"));
			}
			
			return resp;
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(ps != null) ps.close();
			if(conn != null) conn.close();
		}
	}

	public void insertCellResponse(JSONObject resp, Long photoId)  throws Exception {
		Connection conn = null;
		PreparedStatement ps = null;
		
		try {
			
			conn = getConnectionMySql();

			String sql = "replace into foto_cell_info (id_foto, resp) values( ? , ?)";
			
			ps = conn.prepareStatement(sql);
			
			int indice = 1;
			
			ps.setLong(indice++, photoId);
			ps.setString(indice++, resp.toString());
			
			
			int rowsUpdated = ps.executeUpdate();
			
			if(rowsUpdated == 0){
				throw new Exception("Not updated");
			}
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(ps != null) ps.close();
			if(conn != null) conn.close();
		}
	}
	public void updateStatus(JSONObject status, Long idFotoNew) throws Exception {
		Connection conn = null;
		PreparedStatement ps = null;
		
		try {
			
			conn = getConnectionMySql();

			String sql = "update foto_gsa set status = ? where id = ?";
			
			ps = conn.prepareStatement(sql);
			
			int indice = 1;
			
			ps.setString(indice++, status.toString());
			ps.setLong(indice++, idFotoNew);
			
			int rowsUpdated = ps.executeUpdate();
			
			if(rowsUpdated == 0){
				throw new Exception("Not updated");
			}
			
		} catch (Exception e) {
			throw e;
		}
		finally{
			if(ps != null) ps.close();
			if(conn != null) conn.close();
		}
		
	}

}
