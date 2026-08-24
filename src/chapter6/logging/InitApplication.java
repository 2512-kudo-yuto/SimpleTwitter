package chapter6.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class InitApplication {

	/**
	 * シングルトン
	 */
	private static InitApplication instance = null;

	/**
	 * java.util.log.LogManagerに読み込ませるログプロパティファイル
	 */
	public static final String LOG_PROPERTIES_NAME = "logging.properties";

	/**
	 * InitApplicationのインスタンスを返す。
	 * @return InitApplicationのインスタンス
	 */
	public static InitApplication getInstance() {
		if (instance == null) {
			instance = new InitApplication();
		}
		return instance;
	}

	/**
	 * アプリケーションの初期化処理
	 */
	public void init() {
		initLog();
	}

	/**
	 * ログの初期化
	 */
	private void initLog() {

		InputStream in = null;
		try {
			in = InitApplication.class.getClassLoader().getResourceAsStream(LOG_PROPERTIES_NAME);
			if (in == null) {
				System.err.println(LOG_PROPERTIES_NAME + "がクラスパスに存在しません。");
			}
			LogManager.getLogManager().readConfiguration(in);

			//カスタムフォーマットをFileHandlerへ直接登録
			Handler handler = new FileHandler();
			handler.setFormatter(new LogFormatter());

			Logger root = Logger.getLogger("twitter");
			root.setUseParentHandlers(false);
			for (Handler h : root.getHandlers()) {
				if (h instanceof FileHandler) {
					root.removeHandler(h);
				}
			}
			root.addHandler(handler);

			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
