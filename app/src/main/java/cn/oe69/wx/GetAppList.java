package cn.oe69.wx;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GetAppList {
    @GET("api.php")
    Call<AppListBean> get();
}