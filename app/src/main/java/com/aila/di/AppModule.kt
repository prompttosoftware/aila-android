package com.aila.di

import android.content.Context
import com.aila.ai.ASRProcessor
import com.aila.ai.TutorService
import com.aila.ai.stub.AiServiceStub
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideASRProcessor(@ApplicationContext context: Context): ASRProcessor {
        return if (BuildConfig.DEBUG) {
            AiServiceStub()
        } else {
            // Assuming TensorFlowASR implements ASRProcessor and has a factory method
            TensorFlowASR.createFromFile(context, "models/whisper-tiny.tflite")
        }
    }

    @Provides
    @Singleton
    fun provideTutorService(@ApplicationContext context: Context): TutorService {
        return if (BuildConfig.DEBUG) {
            AiServiceStub()
        } else {
            // Assuming MlTaskTutor implements TutorService and has a factory method
            MlTaskTutor.create(context, "models/mt5-small-q.tflite")
        }
    }
}
