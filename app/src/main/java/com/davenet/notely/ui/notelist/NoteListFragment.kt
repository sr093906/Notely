package com.davenet.notely.ui.notelist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.davenet.notely.R
import com.davenet.notely.databinding.FragmentNoteListBinding
import com.davenet.notely.domain.NoteEntry
import com.davenet.notely.ui.NoteListener
import com.davenet.notely.ui.NotesAdapter
import com.davenet.notely.util.Constants
import com.davenet.notely.util.UIState
import com.davenet.notely.util.calculateNoOfColumns
import com.davenet.notely.viewmodels.NoteListViewModel
import com.davenet.notely.viewmodels.NoteListViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_note_list.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.design.longSnackbar


class NoteListFragment : Fragment() {

    private lateinit var noteListViewModel: NoteListViewModel
    private lateinit var uiScope: CoroutineScope
    private lateinit var binding: FragmentNoteListBinding
    private lateinit var coordinator: CoordinatorLayout
    private lateinit var noteList: LiveData<List<NoteEntry>>
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        requireActivity().apply {
            drawer_layout.setDrawerLockMode(LOCK_MODE_UNLOCKED)
            toolbar.isVisible = true
        }
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_note_list, container, false
        )

        auth = FirebaseAuth.getInstance()

        val application = requireNotNull(this.activity).application

        val viewModelFactory = NoteListViewModelFactory(application)

        noteListViewModel =
            ViewModelProvider(this, viewModelFactory).get(NoteListViewModel::class.java)

        val adapter = NotesAdapter(NoteListener {
            noteListViewModel.onNoteClicked(it)
        })

        if (arguments?.getInt(Constants.NOTE_ID) != null) {
            noteListViewModel.onNoteClicked(arguments?.getInt(Constants.NOTE_ID))
        }

        observeViewModel(adapter)

        binding.apply {
            lifecycleOwner = this@NoteListFragment
            uiState = noteListViewModel.uiState
            noteList.adapter = adapter
            noteList.layoutManager =
                GridLayoutManager(context, calculateNoOfColumns(requireContext(), 180))
        }
        return binding.root
    }

    private fun observeViewModel(adapter: NotesAdapter) {
        noteListViewModel.apply {
            notes.observe(viewLifecycleOwner, {
                it?.let {
                    if (it.isNotEmpty()) {
                        noteListViewModel.uiState.set(UIState.HAS_DATA)
                    } else {
                        noteListViewModel.uiState.set(UIState.EMPTY)
                    }
                    adapter.submitToList(it)
                }
            })

            navigateToNoteDetail.observe(viewLifecycleOwner, { noteId ->
                noteId?.let {
                    val bundle = Bundle()
                    bundle.putInt(Constants.NOTE_ID, noteId)
                    findNavController().navigate(
                        R.id.action_noteListFragment_to_editNoteFragment, bundle
                    )
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_noteListFragment_to_editNoteFragment)
        }

        uiScope = CoroutineScope(Dispatchers.Default)

        noteList = noteListViewModel.notes
        coordinator = activity?.findViewById(R.id.list_coordinator)!!
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val noteToErase = noteList.value?.get(position)
                deleteNote(requireContext(), noteToErase!!)
                coordinator.longSnackbar(getString(R.string.note_deleted), getString(R.string.undo)) {
                    restoreNote(requireContext(), noteToErase)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {

                RecyclerViewSwipeDecorator.Builder(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                    .addActionIcon(R.drawable.ic_baseline_delete_24)
                    .setActionIconTint(Color.WHITE)
                    .addBackgroundColor(Color.RED)
                    .create()
                    .decorate()

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }).attachToRecyclerView(binding.noteList)
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                deleteAllNotes()
                undoDeleteNotes(noteList.value!!)
                activity?.invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        noteListViewModel.notes.observe(viewLifecycleOwner, {
            it?.let {
                menu.findItem(R.id.action_clear).isVisible = it.isNotEmpty()
            }
        })
        return super.onPrepareOptionsMenu(menu)
    }

    private fun undoDeleteNotes(noteList: List<NoteEntry>) {
        coordinator.longSnackbar(getString(R.string.notes_deleted), getString(R.string.undo)) {
            insertAllNotes(noteList)
        }
    }

    private fun insertAllNotes(noteList: List<NoteEntry>) {
        uiScope.launch {
            withContext(Dispatchers.Main) {
                noteListViewModel.insertAllNotes(noteList)
            }
        }
    }

    private fun deleteAllNotes() {
        uiScope.launch {
            withContext(Dispatchers.Main) {
                noteListViewModel.deleteAllNotes()
            }
        }
    }

    private fun deleteNote(context: Context, note: NoteEntry) {
        uiScope.launch {
            withContext(Dispatchers.Main) {
                noteListViewModel.deleteNote(context, note)
            }
        }
    }

    private fun restoreNote(context: Context, note: NoteEntry) {
        uiScope.launch {
            withContext(Dispatchers.Main) {
                noteListViewModel.restoreNote(context, note)
            }
        }
    }
}