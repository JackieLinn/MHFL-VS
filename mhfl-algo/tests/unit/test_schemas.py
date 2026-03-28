from utils.schemas import ApiResponse


def test_api_response_success_and_failure_builders():
    ok = ApiResponse.success(data={"x": 1}, message="ok")
    fail = ApiResponse.failure(400, "bad")

    assert ok.code == 200
    assert ok.data == {"x": 1}
    assert ok.message == "ok"
    assert fail.code == 400
    assert fail.data is None
    assert fail.message == "bad"


def test_api_response_common_shortcuts():
    unauthorized = ApiResponse.unauthorized()
    forbidden = ApiResponse.forbidden()
    bad_request = ApiResponse.bad_request()
    internal_error = ApiResponse.internal_error()

    assert unauthorized.code == 401
    assert forbidden.code == 403
    assert bad_request.code == 400
    assert internal_error.code == 500

