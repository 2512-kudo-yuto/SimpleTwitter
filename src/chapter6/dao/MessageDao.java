package chapter6.dao;

import static chapter6.utils.CloseableUtil.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import chapter6.beans.Message;
import chapter6.exception.SQLRuntimeException;
import chapter6.logging.InitApplication;

public class MessageDao {

	/**
	* ロガーインスタンスの生成
	*/
	Logger log = Logger.getLogger("twitter");

	/**
	* デフォルトコンストラクタ
	* アプリケーションの初期化を実施する。
	*/
	public MessageDao() {
		InitApplication application = InitApplication.getInstance();
		application.init();

	}

	public void insert(Connection connection, Message message) {

		log.info(new Object() {
		}.getClass().getEnclosingClass().getName() +
				" : " + new Object() {
				}.getClass().getEnclosingMethod().getName());

		PreparedStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO messages ( ");
			sql.append("    user_id, ");
			sql.append("    text, ");
			sql.append("    created_date, ");
			sql.append("    updated_date ");
			sql.append(") VALUES ( ");
			sql.append("    ?, "); // user_id
			sql.append("    ?, "); // text
			sql.append("    CURRENT_TIMESTAMP, "); // created_date
			sql.append("    CURRENT_TIMESTAMP "); // updated_date
			sql.append(")");

			ps = connection.prepareStatement(sql.toString());

			ps.setInt(1, message.getUserId());
			ps.setString(2, message.getText());

			ps.executeUpdate();
		} catch (SQLException e) {
			log.log(Level.SEVERE, new Object() {
			}.getClass().getEnclosingClass().getName() + " : " + e.toString(), e);
			throw new SQLRuntimeException(e);
		} finally {
			close(ps);
		}
	}

	/**
	 * 指定された投稿IDと投稿者IDに一致するツイートを削除する。
	 *
	 * @param connection データベース接続
	 * @param id 削除対象のツイートID
	 * @param userId ログインユーザーのID
	 */
	public void delete(Connection connection, int id, int userId) {
	    PreparedStatement ps = null;

	    try {
	        ps = connection.prepareStatement(
	                "DELETE FROM messages WHERE id = ? AND user_id = ?");

	        ps.setInt(1, id);
	        ps.setInt(2, userId);

	        ps.executeUpdate();

	    } catch (SQLException e) {
	        throw new SQLRuntimeException(e);

	    } finally {
	        close(ps);
	    }
	}
	
	/**
	 * 指定されたIDのツイートを取得する。
	 *
	 * @param connection データベース接続
	 * @param id 取得対象のツイートID
	 * @return 該当するツイート。該当レコードが存在しない場合はnull
	 */
	public Message select(Connection connection, int id) {

		log.info(new Object(){}.getClass().getEnclosingClass().getName() + 
				" : " + new Object(){}.getClass().getEnclosingMethod().getName());

		PreparedStatement ps = null;
		try {
			String sql = "SELECT * FROM messages WHERE id = ?";
			ps = connection.prepareStatement(sql);
			ps.setInt(1, id);

			ResultSet rs = ps.executeQuery();
			List<Message> messages = toMessageList(rs);
			if (messages.isEmpty()) {
				return null;
			} else {
				return messages.get(0);
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, new Object(){}.getClass().getEnclosingClass().getName() + " : " + e.toString(), e);
			throw new SQLRuntimeException(e);
		} finally {
			close(ps);
		}
	}
	private List<Message> toMessageList(ResultSet rs) throws SQLException {
		List<Message> ret = new ArrayList<Message>();
		while (rs.next()) {
			Message message = new Message();
			message.setId(rs.getInt("id"));
			message.setUserId(rs.getInt("user_id"));
			message.setText(rs.getString("text"));
			message.setCreatedDate(rs.getTimestamp("created_date"));
			message.setUpdatedDate(rs.getTimestamp("updated_date"));

			ret.add(message);
		}
		return ret;
	}
}
