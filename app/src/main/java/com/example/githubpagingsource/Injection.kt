package com.example.githubpagingsource

import androidx.lifecycle.ViewModelProvider
import com.example.githubpagingsource.api.GithubService
import com.example.githubpagingsource.data.GithubRepository
import com.example.githubpagingsource.ui.ViewModelFactory

object Injection {

    private fun provideGithubRepository(): GithubRepository {
        return GithubRepository(GithubService.create())
    }

    fun provideViewModelFactory(): ViewModelProvider.Factory {
        return ViewModelFactory(provideGithubRepository())
    }
}
