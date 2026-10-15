package com.example.notelink.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notelink.data.local.LinkDao
import com.example.notelink.data.model.Link
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LinkViewModel(private val linkDao: LinkDao) : ViewModel() {

    val links: StateFlow<List<Link>> = linkDao.getAllLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLink(name: String, url: String, category: String) {
        viewModelScope.launch {
            linkDao.insertLink(Link(name = name, url = url, category = category))
        }
    }

    fun updateLink(link: Link) {
        viewModelScope.launch {
            linkDao.updateLink(link)
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            linkDao.deleteLink(link)
        }
    }
}

class LinkViewModelFactory(private val linkDao: LinkDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LinkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LinkViewModel(linkDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
