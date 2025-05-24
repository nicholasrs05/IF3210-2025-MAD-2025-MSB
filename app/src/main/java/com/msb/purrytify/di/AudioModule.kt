package com.msb.purrytify.di

import android.content.Context
import com.msb.purrytify.viewmodel.AudioDeviceViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {
    @Provides
    @Singleton
    @Named("receiverViewModel")
    fun provideReceiverViewModel(
        @ApplicationContext context: Context
    ): AudioDeviceViewModel = AudioDeviceViewModel(context)
} 