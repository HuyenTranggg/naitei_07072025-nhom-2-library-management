package com.group2.library_management.common.constants;

/**
 * Lớp final chứa các hằng số cho các endpoints của API.
 * Sử dụng các lớp tĩnh để nhóm các endpoint theo tài nguyên.
 */
public final class Endpoints {

    private Endpoints() {
    }

    private static final String API_BASE = "/api";
    private static final String ADMIN_BASE = "/admin";

    public static final class ApiV1 {
        private static final String BASE = API_BASE + "/v1";

        public static final class Auth {
            private Auth() {}

            public static final String BASE_URL = API_BASE + "/auth";
            public static final String REGISTER_ACTION = "/register";
            public static final String LOGIN_ACTION = "/login";
            public static final String REFRESH_ACTION = "/refresh";
            public static final String LOGOUT_ACTION = "/logout";

            public static final String REGISTER_ENDPOINT = BASE_URL + REGISTER_ACTION;
            public static final String LOGIN_ENDPOINT = BASE_URL + LOGIN_ACTION;
            public static final String REFRESH_ENDPOINT = BASE_URL + REFRESH_ACTION;
            public static final String LOGOUT_ENDPOINT = BASE_URL + LOGOUT_ACTION;
        }

        public static final class Books {
            private Books() {}

            public static final String BASE_URL = BASE + "/books";
            public static final String BY_ID_ACTION = "/{id}";

            public static final String ALL_BOOKS_PATHS = BASE_URL + "/**"; 
        }

        public static final class Cart {
            private Cart() {}
            
            public static final String BASE_URL = BASE + "/cart";
            public static final String ADD_ITEM_ACTION = "/add";
        }

        public static final class BorrowingHistory {
            private BorrowingHistory() {}

            public static final String BASE_URL = BASE + "/borrowing-histories";
            public static final String GET_LIST_ACTION = "";
        }
        
        public static final class Borrowings {
            private Borrowings() {}

            public static final String BASE_URL = BASE + "/borrowings";
            public static final String CREATE_REQUEST_ACTION = "/request";            
            public static final String CANCLE_REQUEST_ACTION = "/requests/{id}";
        }
        
        public static final class Editions {
            private Editions() {}
            
            public static final String BASE_URL = BASE + "/editions";

            public static final String ALL_EDITIONS_PATHS = BASE_URL + "/**"; 
        }
    }

    public static final class Admin {
        public static final String BASE = ADMIN_BASE;
        public static final String ALL_STRINGS = BASE + "/**";
        public static final String BASE_STATIC = "admin";
        public static final String ERROR_STATIC = "error";
        
        public static final class Login{
            private Login() {}
            public static final String failureUrl = BASE + "/login?error=true";
            public static final String LOGIN_ACTION = "/login";
            public static final String ACCESS_DENIED_ACTION = "/access-denied";
            public static final String LOGIN_ACTION_STRINGS = BASE + LOGIN_ACTION;
            public static final String ACCESS_DENIED_ACTION_STRINGS = BASE + ACCESS_DENIED_ACTION;
            public static final String LOGIN_STATIC = BASE_STATIC + LOGIN_ACTION;
            public static final String ACCESS_DENIED_STATIC = BASE_STATIC + ACCESS_DENIED_ACTION;
        }

        public static final class Logout {
            private Logout() {}
            public static final String LOGOUT_ACTION = "/logout";
            public static final String LOGOUT_SUCCESS_ACTION = "/login?logout=true";
            public static final String LOGOUT_ACTION_STRINGS = BASE + LOGOUT_ACTION;
            public static final String LOGOUT_SUCCESS_ACTION_STRINGS = BASE + LOGOUT_SUCCESS_ACTION;
        }

        public static final class Dashboard {
            private Dashboard() {}
            public static final String DASHBOARD_ACTION = "/dashboard";
            public static final String DASHBOARD_ACTION_STRINGS = BASE + DASHBOARD_ACTION;
        }
    }
}
