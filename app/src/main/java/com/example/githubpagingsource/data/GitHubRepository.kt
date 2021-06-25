package com.example.githubpagingsource.data


import android.util.Log
import com.example.githubpagingsource.api.GithubService
import com.example.githubpagingsource.api.IN_QUALIFIER
import com.example.githubpagingsource.model.Repo
import com.example.githubpagingsource.model.RepoSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import retrofit2.HttpException
import java.io.IOException


private const val GITHUB_STARTING_PAGE_INDEX = 1

class GithubRepository(private val service: GithubService) {
    private val inMemoryCache = mutableListOf<Repo>()
    private val searchResults = MutableSharedFlow<RepoSearchResult>(replay = 1)
    private var lastRequestPage = GITHUB_STARTING_PAGE_INDEX
    private var isRequestInProgress = false


    suspend fun getSearchResultStream(query: String):Flow<RepoSearchResult>{
        Log.d("GithubRepository" , "New Query: $query")
        lastRequestPage = 1
        inMemoryCache.clear()
        requestAndSaveData(query)
        return searchResults
    }

    suspend fun requestMore(query: String){
        if(isRequestInProgress) return
        val successful = requestAndSaveData(query)
        if (successful){
            lastRequestPage++
        }
    }

    suspend fun retry(query:String){
        if (isRequestInProgress) return
        requestAndSaveData(query)
    }

    private suspend fun requestAndSaveData(query:String):Boolean{
        isRequestInProgress = true
        var successful = false

        val apiQuery= query+ IN_QUALIFIER
        try {
            val response = service.searchRepos(apiQuery,lastRequestPage, NETWORK_PAGE_SIZE)
            Log.d("GithubRepository" ," response $response")
            val repos= response.items ?: emptyList()
            inMemoryCache.addAll(repos)
            val reposByName = reposByName(query)
            searchResults.emit(RepoSearchResult.Success(reposByName))
            successful=true
        }
        catch (expection: IOException){
            searchResults.emit(RepoSearchResult.Error(expection))
        }
        catch (expection: HttpException){
            searchResults.emit(RepoSearchResult.Error(expection))
        }
        isRequestInProgress = false
        return successful
    }

    private fun reposByName(query: String):List<Repo>{

        return  inMemoryCache.filter {
            it.name.contains(query,true)|| (it.description != null && it.description.contains(query,true))
        }.sortedWith(compareByDescending<Repo>{it.stars}.thenBy {it.name})
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}