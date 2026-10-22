package chapter6.controller;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import chapter6.beans.User;
import chapter6.logging.InitApplication;
import chapter6.service.MessageService;

/**
 * ツイートの削除リクエストを処理するサーブレット。
 * ログインユーザーを確認し、本人が投稿したツイートのみ削除する。
 *
 * @author
 */

@WebServlet(urlPatterns = { "/deleteMessage" })
public class DeleteMessageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	* ロガーインスタンスの生成
	*/
	Logger log = Logger.getLogger("twitter");

	/**
	* デフォルトコンストラクタ
	* アプリケーションの初期化を実施する。
	*/
	public DeleteMessageServlet() {
		InitApplication application = InitApplication.getInstance();
		application.init();
	}

	/**
	 * ツイートの削除リクエストを処理する。
	 * ログインユーザーを確認し、本人の投稿を削除する。
	 *
	 * @param request クライアントからのリクエスト
	 * @param response クライアントへのレスポンス
	 * @throws ServletException サーブレットの処理でエラーが発生した場合
	 * @throws IOException リクエストまたはレスポンスの処理でエラーが発生した場合
	 */
	
	@Override
	protected void doPost(HttpServletRequest request,
	        HttpServletResponse response)
	        throws ServletException, IOException {

	    // セッションとログインユーザーを取得する
	    HttpSession session = request.getSession(false);

	    if (session == null
	            || session.getAttribute("loginUser") == null) {
	        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	        return;
	    }

	    User loginUser = (User) session.getAttribute("loginUser");

	    // 削除対象の投稿IDを取得する
	    String idParam = request.getParameter("id");

	    if (idParam == null || idParam.trim().isEmpty()) {
	        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
	        return;
	    }

	    try {
	        int id = Integer.parseInt(idParam);

	        // ログインユーザーのIDも渡して削除する
	        new MessageService().delete(id, loginUser.getId());

	        response.sendRedirect("./");

	    } catch (NumberFormatException e) {
	        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
	    }
	}
}