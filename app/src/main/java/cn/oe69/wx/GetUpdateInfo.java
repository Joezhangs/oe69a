package cn.oe69.wx;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GetUpdateInfo {
    @GET("update.php")
    Call<UpdateInfoBean> get();
}
