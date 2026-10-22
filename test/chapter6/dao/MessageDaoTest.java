
package chapter6.dao;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import chapter6.beans.Message;
import chapter6.utils.DBUtil;

/**
 * MessageDaoの削除処理に関する単体テストクラス。
 *
 * <p>投稿の削除、削除対象以外の投稿への影響、
 * 存在しない投稿IDを指定した場合の動作、
 * 投稿者IDが異なる場合の削除制御、および
 * 同一投稿を複数回削除した場合の動作を検証する。</p>
 *
 * <p>テストの実行には、JUnit 4とMySQLへの接続環境が必要。</p>
 *
 */
public class MessageDaoTest {

    /** テストで使用するデータベース接続。 */
    private Connection connection;

    /** テスト対象のMessageDao。 */
    private MessageDao messageDao;

    /** テスト用投稿の登録に使用するユーザーID。 */
    private int testUserId;

    /**
     * 各テストの実行前に、データベース接続と
     * MessageDao、およびテスト用ユーザーIDを準備する。
     *
     * @throws SQLException データベースへの接続やSQLの実行に失敗した場合
     */
    @Before
    public void setUp() throws SQLException {
        connection = DBUtil.getConnection();
        connection.setAutoCommit(false);

        messageDao = new MessageDao();
        testUserId = findTestUserId();
    }

    /**
     * 各テストの実行後に、テスト中のデータベース変更を
     * ロールバックし、データベース接続を閉じる。
     *
     * @throws SQLException ロールバックまたは接続のクローズに失敗した場合
     */
    @After
    public void tearDown() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    /**
     * 指定した投稿IDと投稿者IDを使用して、
     * 投稿を正常に削除できることを確認する。
     *
     * <p>【事前条件】テスト用の投稿が登録されていること。</p>
     * <p>【操作】登録した投稿を削除する。</p>
     * <p>【期待結果】削除後、対象の投稿を取得できないこと。</p>
     */
    @Test
    public void testDeleteExistingMessage() throws SQLException {
        int messageId = insertTestMessage("削除対象のテスト投稿");

        messageDao.delete(connection, messageId, testUserId);

        assertNull(
            "削除した投稿は取得できないこと",
            messageDao.select(connection, messageId)
        );
    }

    /**
     * 複数の投稿が存在する場合に、指定した投稿だけが
     * 削除され、ほかの投稿が残ることを確認する。
     *
     * <p>【事前条件】テスト用の投稿が2件登録されていること。</p>
     * <p>【操作】そのうち1件を削除する。</p>
     * <p>【期待結果】削除対象は取得できず、もう1件は取得できること。</p>
     */
    @Test
    public void testDeleteOnlySpecifiedMessage() throws SQLException {
        int deleteId = insertTestMessage("削除する投稿");
        int keepId = insertTestMessage("残す投稿");

        messageDao.delete(connection, deleteId, testUserId);

        assertNull(
            "削除対象は取得できないこと",
            messageDao.select(connection, deleteId)
        );

        assertNotNull(
            "対象外の投稿は残ること",
            messageDao.select(connection, keepId)
        );
    }

    /**
     * 存在しない投稿IDを指定して削除処理を実行しても、
     * ほかの投稿に影響しないことを確認する。
     *
     * <p>【事前条件】テスト用の投稿が1件登録されていること。</p>
     * <p>【操作】存在しない投稿IDを指定して削除する。</p>
     * <p>【期待結果】登録済みの投稿が引き続き取得できること。</p>
     *
     * @throws SQLException データベース操作に失敗した場合
     */
    @Test
    public void testDeleteNonExistingMessage() throws SQLException {
        int keepId = insertTestMessage("残す投稿");
        int nonExistingId = findUnusedMessageId();

        messageDao.delete(connection, nonExistingId, testUserId);

        assertNotNull(
            "別の投稿は削除されないこと",
            messageDao.select(connection, keepId)
        );
    }

    /**
     * 投稿者IDが異なるユーザーを指定して削除処理を実行した場合に、
     * 対象の投稿が削除されないことを確認する。
     *
     * <p>【事前条件】テスト用投稿と、投稿者とは異なるユーザーが存在すること。</p>
     * <p>【操作】別のユーザーIDを指定して削除する。</p>
     * <p>【期待結果】投稿が引き続き取得できること。</p>
     *
     * <p>別のユーザーが存在しない場合は、このテストをスキップする。</p>
     *
     * @throws SQLException データベース操作に失敗した場合
     */
    @Test
    public void testDeleteMessageOfAnotherUser() throws SQLException {
        int messageId = insertTestMessage("他人の投稿");

        int otherUserId = findOtherUserId();

        Assume.assumeTrue(
            "このテストには別のユーザーが必要です",
            otherUserId != -1
        );

        messageDao.delete(connection, messageId, otherUserId);

        assertNotNull(
            "投稿者IDが違う場合は削除されないこと",
            messageDao.select(connection, messageId)
        );
    }

    /**
     * 同一の投稿IDに対して削除処理を2回実行しても例外が発生せず、
     * 対象の投稿が存在しない状態を維持できることを確認する。
     *
     * <p>【事前条件】テスト用の投稿が1件登録されていること。</p>
     * <p>【操作】同じ投稿を2回削除する。</p>
     * <p>【期待結果】2回目の削除後も対象の投稿を取得できないこと。</p>
     *
     * @throws SQLException データベース操作に失敗した場合
     */
    @Test
    public void testDeleteSameMessageTwice() throws SQLException {
        int messageId = insertTestMessage("2回削除する投稿");

        messageDao.delete(connection, messageId, testUserId);
        messageDao.delete(connection, messageId, testUserId);

        assertNull(
            "2回目の削除後も投稿が存在しないこと",
            messageDao.select(connection, messageId)
        );
    }

    /**
     * usersテーブルから、テスト用投稿の登録に使用する
     * ユーザーIDを1件取得する。
     *
     * @return 取得したユーザーID
     * @throws SQLException データベース操作に失敗した場合
     */
    private int findTestUserId() throws SQLException {
        String sql = "SELECT id FROM users LIMIT 1";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            assertTrue(
                "usersテーブルにユーザーを登録してください",
                rs.next()
            );

            return rs.getInt("id");
        }
    }

    /**
     * テスト用の投稿を登録し、自動採番された投稿IDを取得する。
     *
     * @param text テスト用投稿の本文
     * @return 登録された投稿のID
     * @throws SQLException データベース操作に失敗した場合
     */
    private int insertTestMessage(String text) throws SQLException {
        Message message = new Message();
        message.setUserId(testUserId);
        message.setText(text);

        messageDao.insert(connection, message);

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                 "SELECT LAST_INSERT_ID() AS id")) {

            assertTrue("投稿IDを取得できること", rs.next());

            return rs.getInt("id");
        }
    }

    /**
     * テスト用投稿の投稿者とは異なるユーザーIDを取得する。
     *
     * @return 別のユーザーが存在する場合はそのユーザーID、
     *         存在しない場合は-1
     * @throws SQLException データベース操作に失敗した場合
     */
    private int findOtherUserId() throws SQLException {
        String sql = "SELECT id FROM users WHERE id <> "
                   + testUserId + " LIMIT 1";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("id");
            }

            return -1;
        }
    }

    /**
     * messagesテーブルに存在しない投稿IDを取得する。
     *
     * @return 現在の最大投稿IDに1を加えたID
     * @throws SQLException データベース操作に失敗した場合
     */
    private int findUnusedMessageId() throws SQLException {
        String sql =
            "SELECT COALESCE(MAX(id), 0) + 1 AS id FROM messages";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            assertTrue(rs.next());

            return rs.getInt("id");
        }
    }
}