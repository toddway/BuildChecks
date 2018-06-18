package data
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

val okCache10Mb = Cache(File("/tmp"), 10 * 1024 * 1024)

val overrideCacheControlInterceptor : Interceptor = Interceptor { chain ->
    val isOverride = chain.request().header("X-override-cache-control") == "true"
    val cacheControl = chain.request().header("Cache-Control")
    var response = chain.proceed(chain.request())
    if (isOverride && cacheControl != null) {
        response = response.newBuilder()
                .header("Cache-Control", "$cacheControl, X-override-cache-control=true")
                .build()
    }
    response
}

fun retrofit(baseUrl : String, authorization : String): Retrofit {
    val authHeaderInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().addHeader("Authorization", authorization).build()
        chain.proceed(request)
    }

    val authClient = OkHttpClient.Builder()
            .cache(okCache10Mb)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
    authClient.networkInterceptors().add(overrideCacheControlInterceptor)

    val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(authClient.build())
            .build()

    return retrofit
}
