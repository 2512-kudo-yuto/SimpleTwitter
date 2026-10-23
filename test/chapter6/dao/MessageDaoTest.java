package chapter6.dao;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import chapter6.beans.Message;
import chapter6.beans.User;
import chapter6.utils.DBUtil;

/**
 * MessageDaoの削除処理に関するテストクラス。
 *
 * <p>テストごとにテスト用アカウントを2件作成し、
 * 投稿の削除処理と削除対象以外への影響を確認する。</p>
 *
 * <p>テスト終了後はロールバックし、テスト用データを残さない。</p>
 *
 * <p>実行にはJUnit 4とMySQLへの接続環境が必要。</p>
 *
 * @version 1.0
 */
public class MessageDaoTest {

    /** テストで使用するデータベース接続。 */
    private Connection connection;

    /** テスト対象の投稿DAO。 */
    private MessageDao messageDao;

    /** テスト用アカウントを登録するDAO。 */
    private UserDao userDao;

    /** 投稿者として使用するテスト用ユーザーID。 */
    private int testUserId;

    /** 別ユーザーとして使用するテスト用ユーザーID。 */
    private int otherUserId;

    /**
     * 各テストの実行前にDB接続、DAO、テスト用アカウントを準備する。
     *
     * @throws SQLException テスト用ユーザーの登録や取得に失敗した場合
     */
    @Before
    public void setUp() throws SQLException {
        connection = DBUtil.getConnection();
        messageDao = new MessageDao();
        userDao = new UserDao();

        testUserId = insertTestUser("tu");
        otherUserId = insertTestUser("to");
    }

    /**
     * 各テストの実行後に変更をロールバックし、DB接続を閉じる。
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
     * 自分の投稿を正常に削除できることを確認する。
     *
     * <p>【事前条件】テスト用ユーザーの投稿が1件登録されていること。</p>
     * <p>【操作】投稿者IDと投稿IDを指定して削除する。</p>
     * <p>【期待結果】削除した投稿を取得できないこと。</p>
     *
     * @throws SQLException DB操作に失敗した場合
     */
    @Test
    public void testDeleteExistingMessage() throws SQLException {
        int messageId = insertTestMessage(testUserId, "削除対象のテスト投稿");

        messageDao.delete(connection, messageId, testUserId);

        assertNull("削除した投稿は取得できないこと",
                messageDao.select(connection, messageId));
    }

    /**
     * 指定した投稿だけが削除され、ほかの投稿が残ることを確認する。
     *
     * <p>【事前条件】テスト用投稿が2件登録されていること。</p>
     * <p>【操作】そのうち1件を削除する。</p>
     * <p>【期待結果】削除対象は取得できず、もう1件は取得できること。</p>
     *
     * @throws SQLException DB操作に失敗した場合
     */
    @Test
    public void testDeleteOnlySpecifiedMessage() throws SQLException {
        int deleteId = insertTestMessage(testUserId, "削除する投稿");
        int keepId = insertTestMessage(testUserId, "残す投稿");

        messageDao.delete(connection, deleteId, testUserId);

        assertNull("削除対象は取得できないこと",
                messageDao.select(connection, deleteId));
        assertNotNull("対象外の投稿は残ること",
                messageDao.select(connection, keepId));
    }

    /**
     * 存在しない投稿IDを指定しても、ほかの投稿に影響しないことを確認する。
     *
     * <p>【事前条件】テスト用投稿が1件登録されていること。</p>
     * <p>【操作】存在しないID（-1）を指定して削除する。</p>
     * <p>【期待結果】登録済みの投稿が引き続き取得できること。</p>
     *
     * @throws SQLException DB操作に失敗した場合
     */
    @Test
    public void testDeleteNonExistingMessage() throws SQLException {
        int keepId = insertTestMessage(testUserId, "残す投稿");
        int nonExistingId = -1;

        assertNull("削除前から指定IDの投稿が存在しないこと",
                messageDao.select(connection, nonExistingId));

        messageDao.delete(connection, nonExistingId, testUserId);

        assertNotNull("別の投稿は削除されないこと",
                messageDao.select(connection, keepId));
    }

    /**
     * 投稿者と異なるユーザーIDを指定した場合、投稿が削除されないことを確認する。
     *
     * <p>【事前条件】投稿者用と別ユーザー用のテストアカウントが存在すること。</p>
     * <p>【操作】別ユーザーのIDを指定して削除する。</p>
     * <p>【期待結果】元の投稿が引き続き取得できること。</p>
     *
     * @throws SQLException DB操作に失敗した場合
     */
    @Test
    public void testDeleteMessageOfAnotherUser() throws SQLException {
        int messageId = insertTestMessage(testUserId, "他人の投稿");

        messageDao.delete(connection, messageId, otherUserId);

        assertNotNull("投稿者IDが違う場合は削除されないこと",
                messageDao.select(connection, messageId));
    }

    /**
     * 同じ投稿を2回削除しても例外が発生せず、投稿が存在しない状態を維持する。
     *
     * <p>【事前条件】テスト用投稿が1件登録されていること。</p>
     * <p>【操作】同じ投稿IDで削除処理を2回実行する。</p>
     * <p>【期待結果】2回目の削除後も投稿を取得できないこと。</p>
     *
     * @throws SQLException DB操作に失敗した場合
     */
    @Test
    public void testDeleteSameMessageTwice() throws SQLException {
        int messageId = insertTestMessage(testUserId, "2回削除する投稿");

        messageDao.delete(connection, messageId, testUserId);
        messageDao.delete(connection, messageId, testUserId);

        assertNull("2回目の削除後も投稿が存在しないこと",
                messageDao.select(connection, messageId));
    }

    /**
     * テスト用アカウントを作成し、登録されたユーザーIDを取得する。
     *
     * <p>アカウントとメールアドレスにはランダムな文字列を使用し、
     * 既存データとの重複を避ける。</p>
     *
     * @param prefix アカウント名とメールアドレスの先頭に付ける文字列
     * @return 登録されたユーザーID
     * @throws SQLException DB操作に失敗した場合
     */
    private int insertTestUser(String prefix) throws SQLException {
        String token = UUID.randomUUID().toString().replace("-", "");
        String account = prefix + token.substring(0, 16);

        User user = new User();
        user.setAccount(account);
        user.setName("JUnit test");
        user.setEmail(account + "@example.test");
        user.setPassword("test-password");
        user.setDescription("JUnit test account");

        userDao.insert(connection, user);

        String sql = "SELECT id FROM users WHERE account = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, account);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("作成したテスト用アカウントを取得できること", rs.next());
                return rs.getInt("id");
            }
        }
    }

    /**
     * 指定したユーザーのテスト用投稿を登録し、投稿IDを取得する。
     *
     * @param userId 投稿者のユーザーID
     * @param text 投稿本文
     * @return 登録された投稿ID
     * @throws SQLException DB操作に失敗した場合
     */
    private int insertTestMessage(int userId, String text) throws SQLException {
        Message message = new Message();
        message.setUserId(userId);
        message.setText(text);

        messageDao.insert(connection, message);

        try (java.sql.Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID() AS id")) {
            assertTrue("登録した投稿IDを取得できること", rs.next());
            return rs.getInt("id");
        }
    }
}
