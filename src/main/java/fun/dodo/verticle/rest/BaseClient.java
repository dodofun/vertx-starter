package fun.dodo.verticle.rest;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.*;

import java.util.Map;


public interface BaseClient {

    @Headers({
        "User-Agent: dodo"
    })
    @GET("{path}")
    Observable<Response<String>> get(@Path("path") String path, @HeaderMap Map<String, String> headers, @QueryMap Map<String, String> querys);

    @POST("{path}")
    Call<String> post(@Path("path") String path, @Body String body);

    @PUT("{path}")
    Call<String> put(@Path("path") String path);

    @DELETE("/demo/del/{id}")
    Call<String> del(@Path("id") Long id);

}
