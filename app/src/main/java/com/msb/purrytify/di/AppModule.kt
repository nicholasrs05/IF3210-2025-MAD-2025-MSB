package com.msb.purrytify.di

import android.content.Context
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.api.RetrofitClient
import com.msb.purrytify.data.local.PurrytifyDatabase
import com.msb.purrytify.data.local.dao.SongDao
import com.msb.purrytify.data.repository.SongRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val BASE_URL = "http://34.101.226.132:3000"

    // API and Auth related providers
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofitClient: RetrofitClient): ApiService {
        return retrofitClient.createApiService(ApiService::class.java)
    }

    // Database and Repository providers
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
}