package com.sg.exoplayerlearning.di

import android.content.Context
import com.sg.exoplayerlearning.handlers.AudioFocusHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideAudioFocusHandler(
        @ApplicationContext context: Context
    ): AudioFocusHandler {
        return AudioFocusHandler(context)
    }
}