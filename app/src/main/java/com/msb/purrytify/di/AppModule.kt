package com.msb.purrytify.di

import android.content.Context
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.api.AuthInterceptor
import com.msb.purrytify.data.api.RetrofitClient
import com.msb.purrytify.data.api.TokenAuthenticator
import com.msb.purrytify.data.model.OffsetDateTimeAdapter
import com.msb.purrytify.data.storage.DataStoreManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.msb.purrytify.data.local.PurrytifyDatabase
import com.msb.purrytify.data.local.dao.SongDao
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.qr.QRSharingService
import com.msb.purrytify.service.PlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.OffsetDateTime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val BASE_URL = "http://34.101.226.132:3000"

    // API and Auth related providers
    @Provides
    @Singleton
    fun provideMoshiConverterFactory(moshi: Moshi): MoshiConverterFactory {
        return MoshiConverterFactory.create(moshi)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(dataStoreManager: DataStoreManager): AuthInterceptor {
        return AuthInterceptor(dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshiConverterFactory: MoshiConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofitClient: RetrofitClient): ApiService {
        return retrofitClient.createApiService(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(OffsetDateTime::class.java, OffsetDateTimeAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun providePurrytifyDatabase(@ApplicationContext context: Context): PurrytifyDatabase {
        return PurrytifyDatabase.getDatabase(context)
    }

    @Provides
    fun provideSongDao(database: PurrytifyDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    @Singleton
    fun provideSongRepository(songDao: SongDao): SongRepository {
        return SongRepository(songDao)
    }

    @Provides
    @Singleton
    fun provideQRSharingService(@ApplicationContext context: Context): QRSharingService {
        return QRSharingService(context)
    }
    
    @Provides
    @Singleton
    fun providePlayerManager(@ApplicationContext context: Context): PlayerManager {
        return PlayerManager(context)
    }
    
    // AudioService is a system service and will be provided through ServiceConnection
}