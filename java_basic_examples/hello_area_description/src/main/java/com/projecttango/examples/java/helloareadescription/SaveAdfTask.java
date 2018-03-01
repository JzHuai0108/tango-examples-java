/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.examples.java.helloareadescription;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;

import com.google.atap.tango.reconstruction.Tango3dReconstruction;
import com.google.atap.tango.reconstruction.Tango3dReconstructionConfig;
import com.google.atap.tango.reconstruction.Tango3dReconstructionAreaDescription;
import com.google.atap.tango.dataset.TangoDataset;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;
import java.util.Vector;

import static android.content.ContentValues.TAG;

/**
 * Saves the ADF on a background thread and shows a progress dialog while
 * saving.
 */
public class SaveAdfTask extends AsyncTask<Void, Integer, String> {

    /**
     * Listener for the result of the async ADF saving task.
     */
    public interface SaveAdfListener {
        void onSaveAdfFailed(String adfName);
        void onSaveAdfSuccess(String adfName, String adfUuid);
    }

    Context mContext;
    SaveAdfListener mCallbackListener;
    SaveAdfDialog mProgressDialog;
    Tango mTango;
    String mAdfName;

    SaveAdfTask(Context context, SaveAdfListener callbackListener, Tango tango, String adfName) {
        mContext = context;
        mCallbackListener = callbackListener;
        mTango = tango;
        mAdfName = adfName;
        mProgressDialog = new SaveAdfDialog(context);
    }

    /**
     * Sets up the progress dialog.
     */
    @Override
    protected void onPreExecute() {
        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    /**
     * Performs long-running save in the background.
     */
    @Override
    protected String doInBackground(Void... params) {
        String adfUuid = null;
        try {
            // Save the ADF.
            adfUuid = mTango.saveAreaDescription();

            Log.i(TAG, "default export directory:" + TangoDataset.TANGO_DEFAULT_EXPORT_DIRECTORY);
            Tango3dReconstructionConfig config = new Tango3dReconstructionConfig();

            config.putBoolean(Tango3dReconstructionConfig.USE_SPACE_CLEARING, true);
            config.putBoolean("use_floorplan", true);
            config.putBoolean("use_floorplan_canonical_orientation", true);
            Tango3dReconstruction mTango3dReconstruction = new Tango3dReconstruction(config);
            TangoDataset dataset = new TangoDataset(TangoDataset.TANGO_DEFAULT_EXPORT_DIRECTORY,
                    mTango.experimentalGetCurrentDatasetUuid());
//            Tango3dReconstructionAreaDescription areaDescription =
//                    Tango3dReconstructionAreaDescription.createFromDataset(dataset, null, null);

            Log.i(TAG, "found dataset uuid " + mTango.experimentalGetCurrentDatasetUuid());

            List<String> datasets = new Vector<String>(); // = mTango.experimentalListDatasets();
            datasets.add("Witch");
            for (int i = 0; i < datasets.size(); ++i) {
                Log.i(TAG, "found dataset " + i + " name " + datasets.get(i));
            }
            Log.i(TAG, "found dataset count " + datasets.size());

            // Read the ADF Metadata, set the desired name, and save it back.
            TangoAreaDescriptionMetaData metadata = mTango.loadAreaDescriptionMetaData(adfUuid);
            metadata.set(TangoAreaDescriptionMetaData.KEY_NAME, mAdfName.getBytes());
            mTango.saveAreaDescriptionMetadata(adfUuid, metadata);

        } catch (TangoErrorException e) {
            adfUuid = null; // There's currently no additional information in the exception.
        } catch (TangoInvalidException e) {
            adfUuid = null; // There's currently no additional information in the exception.
        }
        return adfUuid;
    }

    /**
     * Responds to progress update events by updating the UI.
     */
    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mProgressDialog != null) {
            mProgressDialog.setProgress(progress[0]);
        }
    }

    /**
     * Dismisses the progress dialog and call the activity.
     */
    @Override
    protected void onPostExecute(String adfUuid) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        if (mCallbackListener != null) {
            if (adfUuid == null) {
                mCallbackListener.onSaveAdfFailed(mAdfName);
            } else {
                mCallbackListener.onSaveAdfSuccess(mAdfName, adfUuid);
            }
        }
    }
}
